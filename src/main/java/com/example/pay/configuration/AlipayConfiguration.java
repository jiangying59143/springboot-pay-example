package com.example.pay.configuration;


import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 两个支付宝客户端，用户可以使用任意一个.
 *
 * alipay-trade-sdk 是对alipay-sdk-java的封装，建议使用alipay-trade-sdk.
 *
 * @author mengday zhang
 */
@Configuration
@EnableConfigurationProperties(AlipayProperties.class)
public class AlipayConfiguration {

	@Autowired
	private AlipayProperties properties;
	/**
	 * alipay-sdk-java
	 * @return
	 */
	@Bean
	public AlipayClient alipayClient(){
		return new DefaultAlipayClient(properties.getGatewayUrl(),
				properties.getAppid(),
				properties.getAppPrivateKey(),
				properties.getFormate(),
				properties.getCharset(),
				properties.getAlipayPublicKey(),
				properties.getSignType());
	}
}
