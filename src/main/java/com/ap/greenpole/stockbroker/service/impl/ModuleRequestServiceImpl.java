package com.ap.greenpole.stockbroker.service.impl;

import java.lang.reflect.InvocationTargetException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.ap.greenpole.stockbroker.dto.Error;
import com.ap.greenpole.stockbroker.dto.Notification;
import com.ap.greenpole.stockbroker.dto.RequestAuthorization;
import com.ap.greenpole.stockbroker.dto.Result;
import com.ap.greenpole.stockbroker.model.ModuleRequest;
import com.ap.greenpole.stockbroker.model.StockBroker;
import com.ap.greenpole.stockbroker.repository.ModuleRequestRepository;
import com.ap.greenpole.stockbroker.service.FilesStorageService;
import com.ap.greenpole.stockbroker.service.ModuleRequestService;
import com.ap.greenpole.stockbroker.service.NotificationPostingsService;
import com.ap.greenpole.stockbroker.utils.BeanUtils;
import com.ap.greenpole.stockbroker.utils.ConstantUtils;
import com.ap.greenpole.stockbroker.utils.Utils;
import com.ap.greenpole.usermodule.model.User;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

@Service
public class ModuleRequestServiceImpl implements ModuleRequestService {

	private static Logger log = LoggerFactory.getLogger(ModuleRequestServiceImpl.class);

	@Autowired
	private ModuleRequestRepository moduleRequestRepository;

	@Autowired
	private StockBrokerServiceImpl stockBrokerService;

	@Autowired
	private FilesStorageService filesStorageService;

	@Autowired
	private NotificationPostingsService notificationPostingsService;

	@Override
	public Long createApprovalRequest(ModuleRequest moduleRequest, List<String> dataOwnerEmail, String dataOwnerName,
			List<String> dataOwnerPhones, User user) {

		log.info("Creating resource {} ", moduleRequest);
		moduleRequest.setStatus(ConstantUtils.PENDING);
		moduleRequest.setCreatedOn(new Date());
		moduleRequest.setRequesterId(user.getId());
		moduleRequest.setModules(ConstantUtils.MODULE);
		moduleRequest.setRequestCode(requestCode(ConstantUtils.MODULE));
		moduleRequest = moduleRequestRepository.save(moduleRequest);
		log.info("Created resource {} ", moduleRequest);

		Notification notification = new Notification(dataOwnerName, user.getFirstName(), dataOwnerPhones,
				dataOwnerEmail, Utils.commaSeperatedToList(user.getPhone()),
				Utils.commaSeperatedToList(user.getEmail()));

		notificationPostingsService.determineVerdictAndCallNotificationService(notification, ConstantUtils.PENDING);

		return moduleRequest.getRequestId();
	}

	private String requestCode(String stockBroker) {
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMddhhmmsss");
		String date = simpleDateFormat.format(new Date());
		return stockBroker + date;
	}

	@Override
	public List<ModuleRequest> getAllApprovalRequest() {
		return moduleRequestRepository.findAllModuleRequestByModulesAndActionRequired(ConstantUtils.MODULE,
				ConstantUtils.REQUEST_TYPES[5]);
	}

	@Override
	public Result<ModuleRequest> getAllApprovalRequest(int pageNumber, int pageSize, Pageable pageable) {

		Page<ModuleRequest> allRecords = moduleRequestRepository.findAllModuleRequestByModulesAndActionRequired(
				ConstantUtils.MODULE, pageable, ConstantUtils.REQUEST_TYPES[5]);
		long noOfRecords = allRecords.getTotalElements();

		return new Result<>(0, allRecords.getContent(), noOfRecords, pageNumber, pageSize);
	}

	@Override
	public ModuleRequest getApprovalRequestById(Long approvalRequestId) {
		return moduleRequestRepository.findModuleRequestByRequestIdAndModulesAndActionRequired(approvalRequestId,
				ConstantUtils.MODULE, ConstantUtils.REQUEST_TYPES[5]);
	}

	@Override
	public void updateApprovalRequest(ModuleRequest moduleRequest) {
		moduleRequest.setApprovedOn(new Date());
		log.info("Updating resource {} ", moduleRequest);
		moduleRequest = moduleRequestRepository.save(moduleRequest);
		log.info("Updated resource {} ", moduleRequest);
	}

	@Override
	public ModuleRequest getApprovalRequestByResourceIdAndActionRequired(ModuleRequest moduleRequest) {
		return moduleRequestRepository
				.findFirstModuleRequestByResourceIdAndActionRequiredAndModulesOrderByRequestIdDesc(
						moduleRequest.getResourceId(), moduleRequest.getActionRequired(), ConstantUtils.MODULE);
	}

	@Override
	public Object authorizeRequest(RequestAuthorization requestAuthorization, User user)
			throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {

		List<Error> errors = new ArrayList<>();
		for (long requestId : requestAuthorization.getRequestIds()) {

			ModuleRequest request = getApprovalRequestById(requestId);
			if (request == null) {
				Error error = new Error(null, "Request ID " + requestId + " does not exist");
				errors.add(error);
			} else {
				if (ConstantUtils.PENDING != request.getStatus()) {
					Error error = new Error(null, "Request ID: " + requestId + " has been "
							+ ConstantUtils.APPROVAL_STATUS[request.getStatus()]);
					errors.add(error);
				} else {
					Gson gson = new GsonBuilder().setDateFormat("MMM dd, yyyy, HH:mm:ss a").create();
					if (ConstantUtils.REQUEST_TYPES[5].equalsIgnoreCase(request.getActionRequired())) {
						StockBroker newRecord = gson.fromJson(request.getNewRecord(), StockBroker.class);
						StockBroker oldRecord = gson.fromJson(request.getOldRecord(), StockBroker.class);
						if (newRecord != null && oldRecord != null) {
							if (ConstantUtils.APPROVAL_ACTIONS[0].equalsIgnoreCase(requestAuthorization.getAction())) {

								newRecord.setSignature(filesStorageService.copy(newRecord.getSignature()));
								newRecord.setSignatureDownloadLink(filesStorageService.resolveDownloadLink(
										newRecord.getSignature(), ConstantUtils.FILE_STORAGE_PATHS[1]));

								BeanUtils<StockBroker> copyNonNull = new BeanUtils<>();
								copyNonNull.copyNonNullProperties(oldRecord, newRecord);

								stockBrokerService.updateStockBroker(oldRecord);
								request = Utils.setRequestStatus(ConstantUtils.ACCEPTED, user.getId(), null, request);
								updateApprovalRequest(request);

								Notification notification = Utils.setUpNotification(oldRecord, user);
								notificationPostingsService.determineVerdictAndCallNotificationService(notification,
										ConstantUtils.ACCEPTED);
							} else {
								request = Utils.setRequestStatus(ConstantUtils.REJECTED, user.getId(),
										requestAuthorization.getRejectionReason(), request);
								updateApprovalRequest(request);

								Notification notification = Utils.setUpNotification(oldRecord, user);
								notificationPostingsService.determineVerdictAndCallNotificationService(notification,
										ConstantUtils.REJECTED);
							}
						}
					}
				}
			}
		}
		return errors;
	}
}
