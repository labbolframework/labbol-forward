/**
 * 
 */
package com.labbol.forward.configuration;

import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.yelong.http.client.HttpClient;

import com.labbol.api.support.client.APIClient;
import com.labbol.api.support.client.APIClientFactory;
import com.labbol.forward.Forwards;

/**
 * @author PengFei
 */
@Configuration
//@ConditionalOnProperty(prefix = Labbol.LABBOL_PROPERTIES_PREFIX, name = "devMode", havingValue = "forward", matchIfMissing = false)
public class ForwardConfiguration {

	/**
	 * @return api 客户端
	 */
	@Bean
	@ConditionalOnProperty(prefix = Forwards.FORWARD_PROPERTIES_PREFIX
			+ ".apiClient", name = "auto", havingValue = "true", matchIfMissing = false)
	public APIClient defaultAPIClient(Environment environment) {
		String prefix = Forwards.FORWARD_PROPERTIES_PREFIX + ".apiClient";
		String serverUrl = environment.getProperty(prefix + ".serverUrl");
		Objects.requireNonNull(serverUrl, "注册APIClient时，发现serverUrl为空。");
		String appKey = environment.getProperty(prefix + ".appKey");
		String appSecret = environment.getProperty(prefix + ".appSecret");
		APIClient defaultAPIClient = APIClientFactory.createSpringMvcDefaultAPIClient(serverUrl, appKey, appSecret);
		Boolean debug = environment.getProperty(prefix + ".debug", Boolean.class, false);
		Logger logger = LoggerFactory.getLogger(APIClient.class);
		if (debug) {
			HttpClient httpClient = defaultAPIClient.getHttpClient();
			httpClient.addHttpRequestInterceptor(x -> {
				logger.info("请求url:" + x.getUrl());
				logger.info("请求参数：" + x.getParams());
				logger.info("请求headers：" + x.getHeaders());
				logger.info("请求body：" + x.getContentStr());
			});
			httpClient.addHttpResponseInterceptor(x -> {
				logger.info("响应状态码：" + x.getResponseCode());
				logger.info("响应headers：" + x.getHeaders());
				logger.info("响应body：" + x.getContentStr());
			});
		}
		return defaultAPIClient;
	}

}
