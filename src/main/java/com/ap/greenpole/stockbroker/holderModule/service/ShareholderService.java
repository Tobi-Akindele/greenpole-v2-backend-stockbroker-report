package com.ap.greenpole.stockbroker.holderModule.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ap.greenpole.stockbroker.dto.Response;
import com.ap.greenpole.stockbroker.holderModule.Entity.Shareholder;
import com.ap.greenpole.stockbroker.holderModule.dao.ShareholderRepository;
import com.ap.greenpole.stockbroker.holderModule.dto.SearchShareholder;
import com.ap.greenpole.stockbroker.service.FilesStorageService;

@Service
public class ShareholderService {
    
    @Autowired
    private ShareholderRepository shareholderRepository;
    
    @Autowired
	private FilesStorageService filesStorageService;
    
    public List<Shareholder> findShareHoldersByStockBroker(long stockBroker){
    	return shareholderRepository.findShareholderByStockBroker(stockBroker);
    }

    public List<Shareholder> searchShareholderByElement(SearchShareholder searchShareholder) {
        return shareholderRepository.searchShareholderByElement(searchShareholder.getElement(), searchShareholder.getValue(),
        		searchShareholder.getStockBroker());
    }
    
    public Response exportQueryResult(SearchShareholder SearchShareholder, String format) {
		Response response = null;
		List<Shareholder> result = searchShareholderByElement(SearchShareholder);
		if(result != null && !result.isEmpty()) {
			response = new Response();
			response.setDownloadUrl(filesStorageService.generateQueryExportFile(result, format, new Shareholder()));
		}
		return response;
	}
}
