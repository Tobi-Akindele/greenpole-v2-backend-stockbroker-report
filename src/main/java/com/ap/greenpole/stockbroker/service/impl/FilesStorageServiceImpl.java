package com.ap.greenpole.stockbroker.service.impl;

import com.ap.greenpole.stockbroker.service.FilesStorageService;
import com.ap.greenpole.stockbroker.utils.*;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Date;
import java.util.List;

@Service
public class FilesStorageServiceImpl implements FilesStorageService {
	
	private static Logger log = LoggerFactory.getLogger(FilesStorageServiceImpl.class);
	
	private final Path rootTmpUploadDir;
	private final Path rootPmtUploadDir;
	private final String resourceLocation;
	private final String downloadServer;
	private final String blobPath;
	private final String blobContainer;

	public FilesStorageServiceImpl(@Value("${file.tmp.upload.dir}") String tmpUploadDir,
			@Value("${file.pmt.upload.dir}") String pmtUploadDir,
			@Value("${resource.location}") String resourceLocation,
			@Value("${download.server}") String downloadServer,
			@Value("${blob.path}") String blobPath,
			@Value("${blob.container}") String blobContainer) {
		super();

		this.rootTmpUploadDir = Paths.get(System.getProperty("user.home") + tmpUploadDir);
		this.rootPmtUploadDir = Paths.get(System.getProperty("user.home") + pmtUploadDir);
		this.resourceLocation = resourceLocation;
		this.downloadServer = downloadServer;
		this.blobPath = blobPath;
		this.blobContainer = blobContainer;

		try {
			Files.createDirectories(rootTmpUploadDir);
			Files.createDirectories(rootPmtUploadDir);
		} catch (IOException e) {
			log.error("Could not initialize folder for {} and {}. {}", rootTmpUploadDir, rootPmtUploadDir, e);
		}
	}

	@Override
	public String save(MultipartFile file, String type, String filename) {
		
		try {
			Path path = null;
			if (ConstantUtils.FILE_STORAGE_PATHS[0].equalsIgnoreCase(type)) {
				path = this.rootTmpUploadDir;
			}
			if (ConstantUtils.FILE_STORAGE_PATHS[1].equalsIgnoreCase(type)) {
				path = this.rootPmtUploadDir;
			}

			BlobServiceClient blobServiceClient = new BlobServiceClientBuilder().connectionString(this.blobPath).buildClient();
			BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(this.blobContainer);
			BlobClient blobClient = containerClient.getBlobClient(filename);

			Files.copy(file.getInputStream(), path.resolve(filename), StandardCopyOption.REPLACE_EXISTING);
			//upload to blob from local path
			blobClient.uploadFromFile(path.resolve(filename).toAbsolutePath().toString());

			return filename;
		} catch (Exception e) {
			log.error("Could not store the file in tmpUploadDir. Error occurred {}", e);
			return null;
		}
	}

	@Override
	public String resolveDownloadLink(String filename, String type) {
		try {
			Path path = null;
			String result = null;
			if (ConstantUtils.FILE_STORAGE_PATHS[0].equalsIgnoreCase(type)) {
				path = this.rootTmpUploadDir;
			}
			if (ConstantUtils.FILE_STORAGE_PATHS[1].equalsIgnoreCase(type)) {
				path = this.rootPmtUploadDir;
			}

			BlobServiceClient blobServiceClient = new BlobServiceClientBuilder().connectionString(this.blobPath).buildClient();
			BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(this.blobContainer);
			BlobClient blobClient = containerClient.getBlobClient(filename);
			
			result = blobClient.getBlobUrl();
			return result;
		} catch (Exception e) {
			log.error("Error occurred {}", e);
			return null;
		}
	}

	@Override
	public void delete(String filename, String type) {
		try {
			Path path = null;
			if (ConstantUtils.FILE_STORAGE_PATHS[0].equalsIgnoreCase(type)) {
				path = this.rootTmpUploadDir;
			}
			if (ConstantUtils.FILE_STORAGE_PATHS[1].equalsIgnoreCase(type)) {
				path = this.rootPmtUploadDir;
			}
			Files.deleteIfExists(path.resolve(filename));
		} catch (IOException e) {
			log.error("Error occurred {}", e);
		}
	}

	@Override
	public String copy(String filename) {
		try {
			Files.move(this.rootTmpUploadDir.resolve(filename),
					this.rootPmtUploadDir.resolve(filename), StandardCopyOption.REPLACE_EXISTING);
			return filename;
		} catch (IOException e) {
			log.error("Could not move. Error occurred {}", e);
			return null;
		}
	}

	@Override
	public <T> String generateQueryExportFile(List<T> data, String format, T entityType) {
		
		IFileGenerator<T> generator = null;
		String downloadUrl = null;
		String directories = System.getProperty("user.home") + resourceLocation + ConstantUtils.DIRECTORY;
		String filename = directories + ConstantUtils.QUERY_EXPORT + "_" + Utils.getDateString(new Date()) + "_"
				+ System.currentTimeMillis();
		filename = FilenameUtils.normalize(filename);
		filename = filename.replaceAll(" ", "_");
		
		try {
			if(ConstantUtils.SUPPORTED_QUERY_EXPORT_FORMATS[0].equalsIgnoreCase(format)) {
				filename += ".csv";
				generator = new CsvFileGenerator<>(filename, entityType);
			}else if(ConstantUtils.SUPPORTED_QUERY_EXPORT_FORMATS[1].equalsIgnoreCase(format)) {
				filename += ".xlsx";
				generator = new ExcelFileGenerator<>(filename, entityType);
			}else if(ConstantUtils.SUPPORTED_QUERY_EXPORT_FORMATS[2].equalsIgnoreCase(format)) {
				filename += ".pdf";
				generator = new PdfFileGenerator<>(filename, entityType);
			}
			
			generator.createDirectory(directories);
			generator.open();
			generator.write(data);
			
			BlobServiceClient blobServiceClient = new BlobServiceClientBuilder().connectionString(this.blobPath).buildClient();
			BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(this.blobContainer);
			BlobClient blobClient = containerClient.getBlobClient(filename);

			Path path = Paths.get(filename);
			//upload to blob from local path
			blobClient.uploadFromFile(path.resolve(filename).toAbsolutePath().toString());
			
			
			downloadUrl = blobClient.getBlobUrl();
			
		} catch (Exception ex) {
			log.error("Error occurred {}", ex);
			throw ex;
		}
		
		return downloadUrl;
	}

}
