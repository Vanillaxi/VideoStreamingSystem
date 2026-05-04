package com.video.config;

import com.alibaba.csp.sentinel.datasource.Converter;
import com.alibaba.csp.sentinel.datasource.ReadableDataSource;
import com.alibaba.csp.sentinel.datasource.nacos.NacosDataSource;
import com.alibaba.csp.sentinel.slots.block.RuleConstant;
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
    private static final String NACOS_ENABLED_KEY = "sentinel.nacos.enabled";

    private SentinelRuleManager() {
    }

    public static void init() {
        initLocalRules();

        boolean nacosEnabled = Boolean.parseBoolean(AppProperties.getProperty(NACOS_ENABLED_KEY, "false"));
        if (!nacosEnabled) {
            log.info("Sentinel Nacos 动态规则已关闭，{}=false，仅使用本地 Sentinel 规则", NACOS_ENABLED_KEY);
            return;
        }

        initNacosRules();
    }

    private static void initLocalRules() {
        FlowRule flowRule = new FlowRule();
        flowRule.setResource(COUPON_SECKILL_PRE_DEDUCT);
        flowRule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        flowRule.setCount(100);
        flowRule.setLimitApp("default");
        flowRule.setStrategy(RuleConstant.STRATEGY_DIRECT);
        flowRule.setControlBehavior(RuleConstant.CONTROL_BEHAVIOR_DEFAULT);
        FlowRuleManager.loadRules(List.of(flowRule));

        ParamFlowRule paramFlowRule = new ParamFlowRule();
        paramFlowRule.setResource(COUPON_SECKILL_PRE_DEDUCT);
        paramFlowRule.setParamIdx(0);
        paramFlowRule.setCount(100);
        paramFlowRule.setDurationInSec(1);
        ParamFlowRuleManager.loadRules(List.of(paramFlowRule));

        DegradeRule slowCallRule = new DegradeRule();
        slowCallRule.setResource(COUPON_SECKILL_PRE_DEDUCT);
        slowCallRule.setGrade(RuleConstant.DEGRADE_GRADE_RT);
        slowCallRule.setCount(300);
        slowCallRule.setTimeWindow(10);
        slowCallRule.setMinRequestAmount(5);
        slowCallRule.setSlowRatioThreshold(0.5);
        slowCallRule.setStatIntervalMs(1000);

        DegradeRule exceptionRatioRule = new DegradeRule();
        exceptionRatioRule.setResource(COUPON_SECKILL_PRE_DEDUCT);
        exceptionRatioRule.setGrade(RuleConstant.DEGRADE_GRADE_EXCEPTION_RATIO);
        exceptionRatioRule.setCount(0.5);
        exceptionRatioRule.setTimeWindow(10);
        exceptionRatioRule.setMinRequestAmount(5);
        exceptionRatioRule.setStatIntervalMs(1000);
        DegradeRuleManager.loadRules(List.of(slowCallRule, exceptionRatioRule));

        log.info("Sentinel 本地规则初始化成功，resource={}, flowQps=100, paramQps=100, degradeRules=2",
                COUPON_SECKILL_PRE_DEDUCT);
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
