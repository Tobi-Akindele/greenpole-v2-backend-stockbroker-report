package com.ap.greenpole.stockbroker.controller;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.ap.greenpole.stockbroker.dto.Response;
import com.ap.greenpole.stockbroker.dto.Result;
import com.ap.greenpole.stockbroker.dto.StockBrokerAPIResponseCode;
import com.ap.greenpole.stockbroker.dto.StockBrokerBaseResponse;
import com.ap.greenpole.stockbroker.holderModule.Entity.Bondholder;
import com.ap.greenpole.stockbroker.holderModule.Entity.Shareholder;
import com.ap.greenpole.stockbroker.holderModule.dto.SearchBondholder;
import com.ap.greenpole.stockbroker.holderModule.dto.SearchShareholder;
import com.ap.greenpole.stockbroker.holderModule.service.BondholderService;
import com.ap.greenpole.stockbroker.holderModule.service.ShareholderService;
import com.ap.greenpole.stockbroker.model.ModuleRequest;
import com.ap.greenpole.stockbroker.model.StockBroker;
import com.ap.greenpole.stockbroker.service.FilesStorageService;
import com.ap.greenpole.stockbroker.service.ModuleRequestService;
import com.ap.greenpole.stockbroker.service.StockBrokerService;
import com.ap.greenpole.stockbroker.utils.ConstantUtils;
import com.ap.greenpole.stockbroker.utils.Utils;
import com.ap.greenpole.usermodule.annotation.PreAuthorizePermission;
import com.ap.greenpole.usermodule.model.User;
import com.ap.greenpole.usermodule.service.UserService;
import com.google.gson.Gson;

/**
 * Created By: Oyindamola Akindele
 * Date: 8/11/2020 2:31 AM
 */

@RestController
@RequestMapping("/api/v1/stockbroker/report")
public class StockBrokerController {

    private static Logger logger = LoggerFactory.getLogger(StockBrokerController.class);

    @Autowired
    private StockBrokerService stockBrokerService;

    @Autowired
    private ModuleRequestService moduleRequestService;
    
    @Autowired
    private ShareholderService shareholderService;
    
    @Autowired
    private BondholderService bondholderService;
    
    @Autowired
	private UserService userService;

	@Autowired
	private FilesStorageService filesStorageService;

	@Value("${file.accepted-types}")
	private String acceptedFileTypes;
	
	@Value("${file.max-upload-size}")
	private String maxFileUploadSize;

	@PostMapping(value = "/search", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorizePermission({"PERMISSION_GET_STOCKBROKER_ACCOUNT"})
    public StockBrokerBaseResponse<Object> searchStockBrokerAccountPaginated(
            @RequestHeader(value = "pageSize", required = false, defaultValue = "" + Integer.MAX_VALUE) String pageSize,
            @RequestHeader(value = "pageNumber", required = false, defaultValue = "1") String pageNumber,
            @RequestHeader(value = "exportQuery", required = false) boolean exportQuery,
            @RequestHeader(value = "format", required = false) String format,
            @RequestBody @Validated StockBroker stockBroker) {
        logger.info("[+] In searchStockBrokerAccountPaginated with pageSize: {},  pageNumber : {}", pageSize, pageNumber);
        StockBrokerBaseResponse<Object> response = new StockBrokerBaseResponse<>();
        response.setStatus(StockBrokerAPIResponseCode.FAILED.getStatus());
        response.setStatusMessage(StockBrokerAPIResponseCode.FAILED.name());
        try {
            if (stockBroker == null) {
                response.setStatusMessage("Request body is missing");
                return response;
            }
            if(stockBroker.getShLowerRange() < 0 || stockBroker.getShUpperRange() < 0) {
            	response.setStatusMessage("Shareholder lower range or upper range is invalid");
                return response;
            }
            if(stockBroker.getBhLowerRange() < 0 || stockBroker.getBhUpperRange() < 0) {
            	response.setStatusMessage("Shareholder lower range is required");
                return response;
            }
            stockBroker.setActive(true);
            if(!exportQuery) {
            	logger.info("[+] Attempting to parse the pagination variables");
                int page = Integer.parseInt(pageNumber);
                int size = Integer.parseInt(pageSize);
                page = Math.max(0, page - 1);
                size = Math.max(1, size);
                Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
                Result<StockBroker> stockBrokerResult = stockBrokerService
                        .searchStockBrokerWithSpecification(stockBroker, Integer.parseInt(pageNumber), Integer.parseInt(pageSize), pageable);
                response.setStatus(StockBrokerAPIResponseCode.SUCCESSFUL.getStatus());
                response.setStatusMessage(StockBrokerAPIResponseCode.SUCCESSFUL.name());
                response.setData(stockBrokerResult);
                return response;
            }else {
            	if(!Arrays.asList(ConstantUtils.SUPPORTED_QUERY_EXPORT_FORMATS).contains(format)) {
            		response.setStatusMessage("File format is required, options include " + Arrays.toString(ConstantUtils.SUPPORTED_QUERY_EXPORT_FORMATS));
                    return response;
            	}
            	
            	Response result = stockBrokerService.exportQueryResult(stockBroker, format);
            	response.setStatus(StockBrokerAPIResponseCode.SUCCESSFUL.getStatus());
                response.setStatusMessage(StockBrokerAPIResponseCode.SUCCESSFUL.name());
                response.setData(result);
                return response;
            }
            
        } catch (NumberFormatException e) {
            logger.error("[+] Error {} occurred while parsing page variable with message: {}",
                    e.getClass().getSimpleName(), e.getMessage());
            response.setStatusMessage("The entered page and size must be integer values.");
        } catch (Exception e) {
            logger.info("[-] Exception happened while searching StockBrokerAccountPaginated {}", e.getMessage());
            response.setStatusMessage("Error Processing Request: " + e.getMessage());
        }
        return response;
    }
    
	@PostMapping(value = "/search/shareholders", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorizePermission({"PERMISSION_GET_STOCKBROKER_ACCOUNT"})
    public StockBrokerBaseResponse<Object> searchStockBrokerShareholderList(
           @RequestHeader(value = "stockBrokerId", required = false) long stockBrokerId,
           @RequestHeader(value = "exportQuery", required = false) boolean exportQuery,
           @RequestHeader(value = "format", required = false) String format,
           @RequestBody @Validated SearchShareholder searchShareholder) {

    	StockBrokerBaseResponse<Object> response = new StockBrokerBaseResponse<>();
        response.setStatus(StockBrokerAPIResponseCode.FAILED.getStatus());
        response.setStatusMessage(StockBrokerAPIResponseCode.FAILED.name());
        try {
        	if(stockBrokerId <= 0) {
        		response.setStatusMessage("Stockbroker account ID is required");
                return response;
        	}
        	
        	StockBroker stockBrokerById = stockBrokerService.getStockBrokerById(stockBrokerId);
        	if(stockBrokerById == null) {
        		response.setStatusMessage("Stockbroker account does not exist");
                return response;
        	}
        	
            if (searchShareholder == null) {
                response.setStatusMessage("Request body is missing");
                return response;
            }
            searchShareholder.setStockBroker(stockBrokerId);
			if (!exportQuery) {

				List<Shareholder> queryResult = shareholderService.searchShareholderByElement(searchShareholder);
				response.setStatus(StockBrokerAPIResponseCode.SUCCESSFUL.getStatus());
				response.setStatusMessage(StockBrokerAPIResponseCode.SUCCESSFUL.name());
				response.setData(queryResult);
				return response;

			} else {
				if(!Arrays.asList(ConstantUtils.SUPPORTED_QUERY_EXPORT_FORMATS).contains(format)) {
            		response.setStatusMessage("File format is required, options include " + Arrays.toString(ConstantUtils.SUPPORTED_QUERY_EXPORT_FORMATS));
                    return response;
            	}
				Response result = shareholderService.exportQueryResult(searchShareholder, format);
            	response.setStatus(StockBrokerAPIResponseCode.SUCCESSFUL.getStatus());
                response.setStatusMessage(StockBrokerAPIResponseCode.SUCCESSFUL.name());
                response.setData(result);
                return response;
			}
			
        } catch (NumberFormatException e) {
            logger.error("[+] Error {} occurred while parsing page variable with message: {}",
                    e.getClass().getSimpleName(), e.getMessage());
            response.setStatusMessage("The entered page and size must be integer values.");
        } catch (Exception e) {
            logger.info("[-] Exception happened while searching Shareholder list {}", e.getMessage());
            response.setStatusMessage("Error Processing Request: " + e.getMessage());
        }
        return response;
    }
    
	@PostMapping(value = "/search/bondholders", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorizePermission({"PERMISSION_GET_STOCKBROKER_ACCOUNT"})
    public StockBrokerBaseResponse<Object> searchStockBrokerBondholderList(
           @RequestHeader(value = "stockBrokerId", required = false) long stockBrokerId,
           @RequestHeader(value = "exportQuery", required = false) boolean exportQuery,
           @RequestHeader(value = "format", required = false) String format,
           @RequestBody @Validated SearchBondholder searchBondholder) {

    	StockBrokerBaseResponse<Object> response = new StockBrokerBaseResponse<>();
        response.setStatus(StockBrokerAPIResponseCode.FAILED.getStatus());
        response.setStatusMessage(StockBrokerAPIResponseCode.FAILED.name());
        try {
        	if(stockBrokerId <= 0) {
        		response.setStatusMessage("Stockbroker account ID is required");
                return response;
        	}
        	
        	StockBroker stockBrokerById = stockBrokerService.getStockBrokerById(stockBrokerId);
        	if(stockBrokerById == null) {
        		response.setStatusMessage("Stockbroker account does not exist");
                return response;
        	}
        	
            if (searchBondholder == null) {
                response.setStatusMessage("Request body is missing");
                return response;
            }
            searchBondholder.setStockBroker(stockBrokerId);
            if (!exportQuery) {
            	
            	List<Bondholder> queryResult = bondholderService.searchBondholderByElement(searchBondholder);

    			response.setStatus(StockBrokerAPIResponseCode.SUCCESSFUL.getStatus());
                response.setStatusMessage(StockBrokerAPIResponseCode.SUCCESSFUL.name());
                response.setData(queryResult);
                return response;
            }else {
            	
            	if(!Arrays.asList(ConstantUtils.SUPPORTED_QUERY_EXPORT_FORMATS).contains(format)) {
            		response.setStatusMessage("File format is required, options include " + Arrays.toString(ConstantUtils.SUPPORTED_QUERY_EXPORT_FORMATS));
                    return response;
            	}
            	Response result = bondholderService.exportQueryResult(searchBondholder, format);
            	response.setStatus(StockBrokerAPIResponseCode.SUCCESSFUL.getStatus());
                response.setStatusMessage(StockBrokerAPIResponseCode.SUCCESSFUL.name());
                response.setData(result);
                return response;
            }
			
        } catch (NumberFormatException e) {
            logger.error("[+] Error {} occurred while parsing page variable with message: {}",
                    e.getClass().getSimpleName(), e.getMessage());
            response.setStatusMessage("The entered page and size must be integer values.");
        } catch (Exception e) {
            logger.info("[-] Exception happened while searching Shareholder list {}", e.getMessage());
            response.setStatusMessage("Error Processing Request: " + e.getMessage());
        }
        return response;
    }
    
	@PostMapping(value = "/signature", produces = MediaType.APPLICATION_JSON_VALUE)
	@PreAuthorizePermission({ "PERMISSION_SIGNATURE_UPLOAD" })
	public StockBrokerBaseResponse<Map<String, Object>> uploadSignature(@RequestParam(name = "file", required = false) MultipartFile file,
			@RequestParam("stockBrokerId") Long stockBrokerId, HttpServletRequest request) {

		StockBrokerBaseResponse<Map<String, Object>> response = new StockBrokerBaseResponse<>();
		response.setStatus(StockBrokerAPIResponseCode.FAILED.getStatus());
        response.setStatusMessage(StockBrokerAPIResponseCode.FAILED.name());

        Optional<User> user = userService.memberFromAuthorization(request.getHeader(ConstantUtils.AUTHORIZATION));
    	if(!user.isPresent()) {
    		response.setStatusMessage("User details not found");
    		return response;
    	}
    	
    	try {
    		if (stockBrokerId == null || stockBrokerId <= 0) {
                response.setStatusMessage("Stockbroker ID is required");
                return response;
            }
        	if (file == null || file.isEmpty()) {
                response.setStatusMessage("file is required");
                return response;
            }
        	if(!Utils.isFileTypeValid(file, acceptedFileTypes)) {
        		response.setStatusMessage("file type is not valid");
                return response;
        	}
        	if(!Utils.isFileSize(file, maxFileUploadSize)) {
        		response.setStatusMessage("Maximum file upload size is " + (Long.valueOf(maxFileUploadSize) / 1000000) + "MB");
                return response;
        	}
        	
        	StockBroker stockBrokerInDb = stockBrokerService.getStockBrokerById(stockBrokerId);
            logger.info("[+] getStockBrokerById in stockBrokerService returned to uploadSignature {}", stockBrokerInDb);
            if (stockBrokerInDb == null) {
                response.setStatusMessage("Stockbroker account does not exist or has been deactivated");
                return response;
            }
            
            ModuleRequest moduleRequest = Utils.checkForOngoingApproval(moduleRequestService, stockBrokerId, ConstantUtils.REQUEST_TYPES[5]);
            logger.info(" checkForOngoingApproval in Utils returned: {}", moduleRequest);
            
			String filename = stockBrokerInDb.getCscsAccountNumber() + "_" + file.getOriginalFilename();
			StockBroker newData = new StockBroker();
			newData.setActive(stockBrokerInDb.getActive());
			newData.setSignature(filesStorageService.save(file, ConstantUtils.FILE_STORAGE_PATHS[0], filename));
			newData.setSignatureDownloadLink(
					filesStorageService.resolveDownloadLink(filename, ConstantUtils.FILE_STORAGE_PATHS[0]));

			Gson gson = new Gson();
            moduleRequest.setOldRecord(gson.toJson(stockBrokerInDb));
            moduleRequest.setNewRecord(gson.toJson(newData));
            
			Long approvalRequestId = moduleRequestService.createApprovalRequest(moduleRequest,
					Utils.commaSeperatedToList(stockBrokerInDb.getEmails()), stockBrokerInDb.getStockBrokerName(),
					Utils.commaSeperatedToList(stockBrokerInDb.getPhoneNumbers()), user.get());
			logger.info(" createApprovalRequest in moduleRequestService returned ID: {} Created", approvalRequestId);
			if (approvalRequestId > 0) {
                response.setStatus(StockBrokerAPIResponseCode.SUCCESSFUL.getStatus());
                response.setStatusMessage(StockBrokerAPIResponseCode.SUCCESSFUL.name());
                Map<String, Object> approvalRequestIdMap = new HashMap<>();
                approvalRequestIdMap.put("id", approvalRequestId);
                response.setData(approvalRequestIdMap);
                return response;
            }
    	}catch(Exception e) {
        	logger.info("[-] Exception happened while uploading signature file {}", e);
        	response.setStatusMessage("Error Processing Request: " + e.getMessage());
        }
    	return response;
    }
}
