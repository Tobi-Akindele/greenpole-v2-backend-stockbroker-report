package com.ap.greenpole.stockbroker.holderModule.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ap.greenpole.stockbroker.dto.Response;
import com.ap.greenpole.stockbroker.holderModule.Entity.Bondholder;
import com.ap.greenpole.stockbroker.holderModule.dao.BondholderRepository;
import com.ap.greenpole.stockbroker.holderModule.dto.SearchBondholder;
import com.ap.greenpole.stockbroker.service.FilesStorageService;

@Service
public class BondholderService {

	@Autowired
	private BondholderRepository bondholderRepository;
	
	@Autowired
	private FilesStorageService filesStorageService;

	public List<Bondholder> findBondholderByStockBroker(long stockBroker) {
		return bondholderRepository.findBondholderByStockBroker(stockBroker);
	}

	public List<Bondholder> searchBondholderByElement(SearchBondholder searchBondholder) {
		return bondholderRepository.searchBondholderByElement(searchBondholder.getElement(),
				searchBondholder.getValue(), searchBondholder.getStockBroker());
	}
	
	public Response exportQueryResult(SearchBondholder searchBondholder, String format) {
		Response response = null;
		List<Bondholder> result = searchBondholderByElement(searchBondholder);
		if(result != null && !result.isEmpty()) {
			response = new Response();
			response.setDownloadUrl(filesStorageService.generateQueryExportFile(result, format, new Bondholder()));
		}
		return response;
	}
}
