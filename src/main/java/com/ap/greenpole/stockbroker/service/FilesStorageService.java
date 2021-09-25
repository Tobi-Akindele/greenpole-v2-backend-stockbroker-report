package com.ap.greenpole.stockbroker.service;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

public interface FilesStorageService {

	public String save(MultipartFile file, String type, String filename);

	public String copy(String filename);

	public String resolveDownloadLink(String filename, String type);

	public void delete(String filename, String type);

	public <T> String generateQueryExportFile(List<T> data, String format, T entityType);
}
