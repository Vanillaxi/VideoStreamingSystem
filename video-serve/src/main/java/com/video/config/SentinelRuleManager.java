package com.video.config;

import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowItem;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowRuleManager;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
public class SentinelRuleManager {
    public static final String COUPON_SECKILL_PRE_DEDUCT = "coupon_seckill_pre_deduct";

    private SentinelRuleManager() {
    }

    public static void init() {
        initFlowRules();
        initHotParamRules();
    }

    private static void initFlowRules() {
        FlowRule rule = new FlowRule();
        rule.setResource(COUPON_SECKILL_PRE_DEDUCT);
        rule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        rule.setCount(100);
        FlowRuleManager.loadRules(Collections.singletonList(rule));
        log.info("Sentinel 规则初始化完成，resource={}, qps={}", COUPON_SECKILL_PRE_DEDUCT, 100);
    }

    private static void initHotParamRules() {
        ParamFlowRule rule = new ParamFlowRule(COUPON_SECKILL_PRE_DEDUCT);
        rule.setParamIdx(0);
        rule.setCount(100);

        ParamFlowItem couponFiveRule = new ParamFlowItem();
        couponFiveRule.setObject("5");
        couponFiveRule.setClassType(Long.class.getName());
        couponFiveRule.setCount(20);

        List<ParamFlowItem> items = new ArrayList<>();
        items.add(couponFiveRule);
        rule.setParamFlowItemList(items);

        ParamFlowRuleManager.loadRules(Collections.singletonList(rule));
        log.info("Sentinel 热点参数规则初始化完成，resource={}, paramIdx=0, defaultQps=100, couponId=5 qps=20",
                COUPON_SECKILL_PRE_DEDUCT);
    }
}
