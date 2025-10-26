package com.example.usermanagement.service;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.example.usermanagement.exception.CustomExceptions;

import io.imagekit.sdk.ImageKit;
import io.imagekit.sdk.config.Configuration;
import io.imagekit.sdk.models.FileCreateRequest;
import io.imagekit.sdk.models.results.Result;

@Service
public class ImageKitService {

	@Value("${imagekit.private.key}")
	private String privateKey;

	@Value("${imagekit.public.key}")
	private String publicKey;

	@Value("${imagekit.url.endpoint}")
	private String urlEndpoint;

	public Map<String, String> uploadImage(MultipartFile file, String folder) {
		try {

			ImageKit imageKit = ImageKit.getInstance();
			Configuration config = new Configuration(publicKey, privateKey, urlEndpoint);
			imageKit.setConfig(config);

			byte[] fileBytes = file.getBytes();
			String base64 = Base64.getEncoder().encodeToString(fileBytes);

			FileCreateRequest fileCreateRequest = new FileCreateRequest(base64, file.getOriginalFilename());
			Result result = imageKit.upload(fileCreateRequest);

			Map<String, String> response = new HashMap<>();
			response.put("fileId", result.getFileId());
			response.put("url", result.getUrl());
			response.put("name", result.getName());

			return response;
		} catch (Exception e) {
			throw new CustomExceptions.FileUploadException("Failed to upload image: " + e.getMessage());
		}
	}

	public void deleteImage(String fileId) {
		try {
			ImageKit.getInstance().deleteFile(fileId);
		} catch (Exception e) {
			throw new CustomExceptions.FileUploadException("Failed to delete image: " + e.getMessage());
		}
	}
}