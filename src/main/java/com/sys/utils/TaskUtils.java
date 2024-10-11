package com.sys.utils;

import com.sys.entity.Dept;
import com.sys.mapper.DeptMapper;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;

/*
        张睿相   Java


        事项工具类
*/
public class TaskUtils {

    /**
     * <h1>获取参数校验错误信息</h1>
     * <hr/>
     * 用于获取参数校验错误信息
     *
     * @param bindingResult 参数校验结果
     * @return {@link ArrayList<String>}
     * @since v1.0.0
     */
    public static @NotNull String getValidatedErrorList(BindingResult bindingResult) {
        String msg = "";
        for (ObjectError objectError : bindingResult.getAllErrors()) {
            msg += objectError.getDefaultMessage()+"\n";
        }
        return msg;
    }

    // 督办单位状态转换方法
    public static String generateStatusStringForSupervisor(String status) {
        switch (status) {
            case "1":
                return "待交办";
            case "2":
                return "待领办";
            case "3":
                return "领办逾期";
            case "4":
                return "已领办";
            case "5":
            case "6":
                return "推进中";
            case "7":
                return "反馈逾期";
            case "8":
                return "已反馈";
            case "9":
                return "反馈驳回";
            case "10":
                return "已办结";
            case "11":
                return "已终止";
            default:
                return "未知状态";
        }
    }

    // 主办单位负责人方法
    public static String generateStatusStringForOrganizerLeader(String status) {
        switch (status) {
            case "2":
                return "待领办";
            case "3":
                return "领办逾期";
            case "4":
                return "已领办";
            case "5":
                return "待审批";
            case "6":
                return "反馈驳回";
            case "7":
                return "上报逾期";
            case "8":
                return "已审批";
            case "9":
                return "上报驳回";
            default:
                return "未知状态";
        }
    }

    //  主办单位领导状态转换
    public static String generateStatusStringForOrganizer(String status) {
        switch (status) {
            case "3":
                return "待领办";
            case "4":
                return "已领办";
            case "5":
                return "已分配";
            case "6":
                return "已执行";
            case "7":
                return "已驳回";
            case "8":
                return "已提交";
            case "9":
                return "待领办";
            case "14":
                return "领办逾期";
            case "15":
                return "反馈逾期";
            default:
                return "未知状态";
        }
    }

    //    字符串转换为数组   "1","2" -->  [1,2]
    public static Integer[] convertToIntArray(String str) {

        if(str == null || str.equals("")){
            return new Integer[0];
        }

        String[] strArray = new String[0];
        if(str != null) {
            strArray = str.split(",");
        }
        Integer[] intArray = new Integer[strArray.length];
        for (int i = 0; i < strArray.length; i++) {
            intArray[i] = Integer.parseInt(strArray[i].trim());
        }
        return intArray;
    }

//    转换是否事项重大数据
    public static String converToStringForIsVip(String isVip){
        if(isVip.equals("true")){
            return "是";
        }
        if(isVip.equals("false")) {
            return "否";
        }
        return "未知";
    }

}
