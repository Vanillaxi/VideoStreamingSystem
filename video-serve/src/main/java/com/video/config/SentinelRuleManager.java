package com.video.config;

import com.alibaba.csp.sentinel.datasource.Converter;
import com.alibaba.csp.sentinel.datasource.ReadableDataSource;
import com.alibaba.csp.sentinel.datasource.nacos.NacosDataSource;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRule;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRuleManager;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowRuleManager;
import com.alibaba.fastjson.JSON;
import com.video.utils.AppProperties;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class SentinelRuleManager {
    public static final String COUPON_SECKILL_PRE_DEDUCT = "coupon_seckill_pre_deduct";
    private static final String DEFAULT_SERVER_ADDR = "localhost:8848";
    private static final String DEFAULT_GROUP_ID = "DEFAULT_GROUP";
    private static final String DEFAULT_FLOW_DATA_ID = "video-system-flow-rules";
    private static final String DEFAULT_PARAM_FLOW_DATA_ID = "video-system-param-flow-rules";
    private static final String DEFAULT_DEGRADE_DATA_ID = "video-system-degrade-rules";

    private SentinelRuleManager() {
    }

    public static void init() {
        initNacosRules();
    }

    private static void initNacosRules() {
        String serverAddr = AppProperties.getProperty("nacos.serverAddr", DEFAULT_SERVER_ADDR);
        String groupId = AppProperties.getProperty("sentinel.nacos.group", DEFAULT_GROUP_ID);
        initNacosFlowRules(serverAddr, groupId,
                AppProperties.getProperty("sentinel.nacos.flowDataId", DEFAULT_FLOW_DATA_ID));
        initNacosHotParamRules(serverAddr, groupId,
                AppProperties.getProperty("sentinel.nacos.paramFlowDataId", DEFAULT_PARAM_FLOW_DATA_ID));
        initNacosDegradeRules(serverAddr, groupId,
                AppProperties.getProperty("sentinel.nacos.degradeDataId", DEFAULT_DEGRADE_DATA_ID));
    }

    private static void initNacosFlowRules(String serverAddr, String groupId, String dataId) {
        try {
            Converter<String, List<FlowRule>> parser = source -> JSON.parseArray(source, FlowRule.class);
            ReadableDataSource<String, List<FlowRule>> dataSource = new NacosDataSource<>(serverAddr, groupId, dataId, parser);
            FlowRuleManager.register2Property(dataSource.getProperty());
            log.info("Sentinel Nacos 流控规则数据源初始化成功，serverAddr={}, groupId={}, dataId={}",
                    serverAddr, groupId, dataId);
        } catch (Exception e) {
            log.error("Sentinel Nacos 流控规则数据源初始化失败，serverAddr={}, groupId={}, dataId={}",
                    serverAddr, groupId, dataId, e);
            throw e;
        }
    }

    private static void initNacosHotParamRules(String serverAddr, String groupId, String dataId) {
        try {
            Converter<String, List<ParamFlowRule>> parser = source -> JSON.parseArray(source, ParamFlowRule.class);
            ReadableDataSource<String, List<ParamFlowRule>> dataSource = new NacosDataSource<>(serverAddr, groupId, dataId, parser);
            ParamFlowRuleManager.register2Property(dataSource.getProperty());
            log.info("Sentinel Nacos 热点参数规则数据源初始化成功，serverAddr={}, groupId={}, dataId={}",
                    serverAddr, groupId, dataId);
        } catch (Exception e) {
            log.error("Sentinel Nacos 热点参数规则数据源初始化失败，serverAddr={}, groupId={}, dataId={}",
                    serverAddr, groupId, dataId, e);
            throw e;
        }
    }

    private static void initNacosDegradeRules(String serverAddr, String groupId, String dataId) {
        try {
            Converter<String, List<DegradeRule>> parser = source -> JSON.parseArray(source, DegradeRule.class);
            ReadableDataSource<String, List<DegradeRule>> dataSource = new NacosDataSource<>(serverAddr, groupId, dataId, parser);
            DegradeRuleManager.register2Property(dataSource.getProperty());
            log.info("Sentinel Nacos 熔断规则数据源初始化成功，serverAddr={}, groupId={}, dataId={}",
                    serverAddr, groupId, dataId);
        } catch (Exception e) {
            log.error("Sentinel Nacos 熔断规则数据源初始化失败，serverAddr={}, groupId={}, dataId={}",
                    serverAddr, groupId, dataId, e);
            throw e;
        }
    }
}
