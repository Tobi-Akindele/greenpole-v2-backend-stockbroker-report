package com.ap.greenpole.stockbroker.service;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

import org.springframework.data.domain.Pageable;

import com.ap.greenpole.stockbroker.dto.RequestAuthorization;
import com.ap.greenpole.stockbroker.dto.Result;
import com.ap.greenpole.stockbroker.model.ModuleRequest;
import com.ap.greenpole.usermodule.model.User;

public interface ModuleRequestService {

	Long createApprovalRequest(ModuleRequest moduleRequest, List<String> dataOwnerEmail, String dataOwnerName,
			List<String> dataOwnerPhones, User user);

	List<ModuleRequest> getAllApprovalRequest();

	Result<ModuleRequest> getAllApprovalRequest(int pageNumber, int pageSize, Pageable pageable);

	ModuleRequest getApprovalRequestById(Long approvalRequestId);

	Object authorizeRequest(RequestAuthorization requestAuthorization, User user)
			throws IllegalAccessException, InvocationTargetException, NoSuchMethodException;

	void updateApprovalRequest(ModuleRequest moduleRequest);

	ModuleRequest getApprovalRequestByResourceIdAndActionRequired(ModuleRequest moduleRequest);

}
