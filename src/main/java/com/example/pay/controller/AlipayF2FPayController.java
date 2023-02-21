package com.example.pay.controller;

import com.alibaba.fastjson.JSONObject;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.domain.GoodsDetail;
import com.alipay.api.request.AlipayTradePayRequest;
import com.alipay.api.request.AlipayTradePrecreateRequest;
import com.alipay.api.request.AlipayTradeRefundRequest;
import com.alipay.api.request.AlipayUserTwostageCommonUseRequest;
import com.alipay.api.response.AlipayTradePayResponse;
import com.alipay.api.response.AlipayTradePrecreateResponse;
import com.alipay.api.response.AlipayTradeRefundResponse;
import com.alipay.api.response.AlipayUserTwostageCommonUseResponse;
import com.example.pay.configuration.AlipayProperties;
import com.example.pay.util.PayUtil;
import com.example.pay.util.ZxingUtils;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.ResourceUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.util.*;

/**
 * 支付宝-当面付 控制器.
 * <p>
 * https://openclub.alipay.com/read.php?tid=1720&fid=40
 *
 * https://docs.open.alipay.com/203/105910
 *
 * @author Mengday Zhang
 * @version 1.0
 * @since 2018/6/4
 */
@RestController
@RequestMapping("/alipay/f2fpay")
public class AlipayF2FPayController {
    private static Logger log = LoggerFactory.getLogger(AlipayF2FPayController.class);

    @Autowired
    private AlipayClient alipayClient;

    @Autowired
    private AlipayProperties aliPayProperties;


    /**
     * 当面付-条码付
     *
     * 商家使用扫码工具(扫码枪等)扫描用户支付宝的付款码
     *
     * @param authCode
     */
    @PostMapping("/barCodePay")
    public String barCodePay(String authCode) throws AlipayApiException {
        // 实际使用时需要根据商品id查询商品的基本信息并计算价格(可能还有什么优惠)，这里只是写死值来测试

        // (必填) 商户网站订单系统中唯一订单号，64个字符以内，只能包含字母、数字、下划线，
        String outTradeNo = UUID.randomUUID().toString();

        // (必填) 订单标题，粗略描述用户的支付目的。如“喜士多（浦东店）消费”
        String subject = "测试订单";

        // 订单描述，可以对交易或商品进行一个详细地描述，比如填写"购买商品2件共15.00元"
        String body = "购买商品2件共20.05元";

        // (必填) 订单总金额，单位为元，不能超过1亿元
        // 如果同时传入了【打折金额】,【不可打折金额】,【订单总金额】三者,则必须满足如下条件:【订单总金额】=【打折金额】+【不可打折金额】
        String totalAmount = "0.01";

        // (可选) 订单不可打折金额，可以配合商家平台配置折扣活动，如果酒水不参与打折，则将对应金额填写至此字段
        // 如果该值未传入,但传入了【订单总金额】,【打折金额】,则该值默认为【订单总金额】-【打折金额】
        String undiscountableAmount = "";

        // (必填) 商户门店编号，通过门店号和商家后台可以配置精准到门店的折扣信息，详询支付宝技术支持
        String storeId = "test_store_id";

        // 商户操作员编号，添加此参数可以为商户操作员做销售统计
        String operatorId = "test_operator_id";


        //// 扩展信息
        //JSONObject extendParams = new JSONObject();
        //extendParams.put("sys_service_provider_id", "2088511833207846");
        //bizContent.put("extend_params", extendParams);

        //// 返回参数选项
        //JSONArray queryOptions = new JSONArray();
        //queryOptions.add("fund_bill_list");
        //queryOptions.add("voucher_detail_list");
        //bizContent.put("query_options", queryOptions);

        AlipayTradePayRequest alipayTradePayRequest = new AlipayTradePayRequest();
        JSONObject bizContent = new JSONObject();
        bizContent.put("out_trade_no", outTradeNo);
        bizContent.put("total_amount", totalAmount);
        bizContent.put("subject", subject);
        bizContent.put("scene", "bar_code");
        //支付授权码。
        //当面付场景传买家的付款码（25~30开头的长度为16~24位的数字，实际字符串长度以开发者获取的付款码长度为准）或者刷脸标识串（fp开头的35位字符串）。
        bizContent.put("auth_code", "28763443825664394");

        // 商品明细列表，需填写购买商品详细信息，
        List<GoodsDetail> goodsDetailList = new ArrayList<>();
        //// 商品明细信息
        GoodsDetail goods1 = new GoodsDetail();
        goods1.setGoodsId("goods_id001");
        goods1.setGoodsName("全麦小面包");
        goods1.setQuantity(1L);
        goods1.setPrice("0.01");
        goodsDetailList.add(goods1);

        GoodsDetail goods2 = new GoodsDetail();
        goods2.setGoodsId("goods_id002");
        goods2.setGoodsName("黑人牙刷");
        goods2.setQuantity(1L);
        goods2.setPrice("0.01");
        goodsDetailList.add(goods2);
        bizContent.put("goods_detail", goodsDetailList);

        // 当面付，面对面付，face to face pay -> face 2 face pay -> f2f pay
        // 同步返回支付结果
        AlipayTradePayResponse f2FPayResult = alipayClient.execute(alipayTradePayRequest);
        // 注意：一定要处理支付的结果，因为不是每次支付都一定会成功，可能会失败
        if (f2FPayResult.isSuccess()) {
            log.info("支付宝支付成功: )");
        }else{
            log.info("调用失败: )");
        }

        /**
         * {
         *   "alipay_trade_pay_response": {
         *     "code": "10000",
         *     "msg": "Success",
         *     "buyer_logon_id": "ekf***@sandbox.com",
         *     "buyer_pay_amount": "0.01",
         *     "buyer_user_id": "2088102176027680",
         *     "buyer_user_type": "PRIVATE",
         *     "fund_bill_list": [
         *       {
         *         "amount": "0.01",
         *         "fund_channel": "ALIPAYACCOUNT"
         *       }
         *     ],
         *     "gmt_payment": "2018-06-10 14:54:16",
         *     "invoice_amount": "0.01",
         *     "out_trade_no": "91fbd3fa-8558-443a-82c2-bd8e941d7e71",
         *     "point_amount": "0.00",
         *     "receipt_amount": "0.01",
         *     "total_amount": "0.01",
         *     "trade_no": "2018061021001004680200326495"
         *   },
         *   "sign": "BNgMmA2t8fwFZNSa39kyEKgL6hV45DVOKOsdyyzTzsQnX8HEkKOzVevQEDyK083dNYewip1KK/K92BTDY2KMAsrOEqcCNxsk9NLAvK9ZQVxQzFbAFKqs5EBAEzncSWnChJcb7VMhDakUxHZFmclHg38dLJiHE2bEcF8ar9R1zj0p4V0Jr+BXO10kLtaSTc9NeaCwJZ89sPHKitNnUWRroU7t0xPHc1hWpstObwixKmAWnsFyb9eyGwPQnqNBsUVNSNWCJ7Pg3rb03Tx6J3zNsqH5f0YhWilMi09npPe33URFc6zG1HJSfhEm4Gq1zwQrPoA/anW8BbdmEUUmNo1dEw=="
         * }
         */
        String result = f2FPayResult.getBody();

        return result;
    }

    /**
     * 当面付-扫码付
     *
     * 扫码支付，指用户打开支付宝钱包中的“扫一扫”功能，扫描商户针对每个订单实时生成的订单二维码，并在手机端确认支付。
     *
     * 发起预下单请求，同步返回订单二维码
     *
     * 适用场景：商家获取二维码展示在屏幕上，然后用户去扫描屏幕上的二维码
     * @return
     * @throws AlipayApiException
     */
    @PostMapping("/precreate")
    public void precreate(HttpServletRequest request, HttpServletResponse response) throws Exception {
        // 实际使用时需要根据商品id查询商品的基本信息并计算价格(可能还有什么优惠)，这里只是写死值来测试

        // (必填) 商户网站订单系统中唯一订单号，64个字符以内，只能包含字母、数字、下划线，
        String outTradeNo = UUID.randomUUID().toString();

        // (必填) 订单标题，粗略描述用户的支付目的。如“喜士多（浦东店）消费”
        String subject = "测试订单";

        // 订单描述，可以对交易或商品进行一个详细地描述，比如填写"购买商品2件共15.00元"
        String body = "购买商品2件共20.05元";

        // (必填) 订单总金额，单位为元，不能超过1亿元
        // 如果同时传入了【打折金额】,【不可打折金额】,【订单总金额】三者,则必须满足如下条件:【订单总金额】=【打折金额】+【不可打折金额】
        String totalAmount = "0.01";

        // (可选) 订单不可打折金额，可以配合商家平台配置折扣活动，如果酒水不参与打折，则将对应金额填写至此字段
        // 如果该值未传入,但传入了【订单总金额】,【打折金额】,则该值默认为【订单总金额】-【打折金额】
        String undiscountableAmount = "";

        // 卖家支付宝账号ID，用于支持一个签约账号下支持打款到不同的收款账号，(打款到sellerId对应的支付宝账号)
        // 如果该字段为空，则默认为与支付宝签约的商户的PID，也就是appid对应的PID
        String sellerId = "";

        // (必填) 商户门店编号，通过门店号和商家后台可以配置精准到门店的折扣信息，详询支付宝技术支持
        String storeId = "test_store_id";

        // 商户操作员编号，添加此参数可以为商户操作员做销售统计
        String operatorId = "test_operator_id";


        // 商品明细列表，需填写购买商品详细信息，
        List<GoodsDetail> goodsDetailList = new ArrayList<>();
        //// 商品明细信息
        GoodsDetail goods1 = new GoodsDetail();
        goods1.setGoodsId("goods_id001");
        goods1.setGoodsName("全麦小面包");
        goods1.setQuantity(1L);
        goods1.setPrice("0.01");
        goodsDetailList.add(goods1);

        GoodsDetail goods2 = new GoodsDetail();
        goods2.setGoodsId("goods_id002");
        goods2.setGoodsName("黑人牙刷");
        goods2.setQuantity(1L);
        goods2.setPrice("0.01");
        goodsDetailList.add(goods2);

        AlipayTradePrecreateRequest alipayTradePrecreateRequest = new AlipayTradePrecreateRequest();
        alipayTradePrecreateRequest.setNotifyUrl(aliPayProperties.getNotifyUrl());
        JSONObject bizContent = new JSONObject();
        bizContent.put("out_trade_no", "20210817010101003");
        bizContent.put("total_amount", 0.01);
        bizContent.put("subject", "测试商品");
        bizContent.put("goods_detail", goodsDetailList);

        AlipayTradePrecreateResponse alipayTradePrecreateResponse = alipayClient.execute(alipayTradePrecreateRequest);
        String qrCodeUrl = null;
            if(alipayTradePrecreateResponse.isSuccess()){
            log.info("支付宝预下单成功: )");

            File file = ResourceUtils.getFile(ResourceUtils.CLASSPATH_URL_PREFIX + "images/");
            if (!file.exists()) {
                file.mkdirs();
            }
            String absolutePath = file.getAbsolutePath();
            String fileName = String.format("%sqr-%s.png", File.separator, alipayTradePrecreateResponse.getOutTradeNo());
            String filePath = new StringBuilder(absolutePath).append(fileName).toString();

            // 这里只是演示将图片写到服务器中，实际可以返回二维码让前端去生成
            String basePath = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort() + request.getContextPath() + "/";
            qrCodeUrl = basePath + fileName;
            response.getWriter().write("<img src=\"" + qrCodeUrl + "\" />");
            ZxingUtils.getQRCodeImge(alipayTradePrecreateResponse.getQrCode(), 256, filePath);
        }else{
            log.error("支付宝预下单失败!!!");
        }

    }

    /**
     * 退款
     * @param orderNo 商户订单号
     * @return
     */
    @PostMapping("/refund")
    public String refund(String orderNo) throws AlipayApiException {
        AlipayTradeRefundRequest request = new AlipayTradeRefundRequest();
        JSONObject bizContent = new JSONObject();
        bizContent.put("trade_no", orderNo);
        bizContent.put("refund_amount", 0.01);
        bizContent.put("out_request_no", "HZ01RF001");

        request.setBizContent(bizContent.toString());
        AlipayTradeRefundResponse response = alipayClient.execute(request);
        if(response.isSuccess()){
            log.info("支付宝退款成功: )");
        } else {
            log.error("支付宝退款失败!!!");
        }
        return response.getBody();
    }
}
