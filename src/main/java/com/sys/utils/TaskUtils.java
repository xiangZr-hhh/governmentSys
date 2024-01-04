package com.sys.utils;

import com.sys.entity.Dept;
import com.sys.mapper.DeptMapper;

import java.util.ArrayList;
import java.util.List;

/*
        张睿相   Java


        事项工具类
*/
public class TaskUtils {

    // 督办人员方法
    public static String generateStatusStringForSupervisor(String status) {
        switch (status) {
            case "1":
                return "待交办";
            case "2":
                return "已驳回";
            case "3":
                return "已交办";
            case "4":
                return "已领办";
            case "5":
            case "6":
            case "7":
            case "8":
            case "9":
                return "办理中";
            case "10":
                return "已办结";
            case "11":
                return "已归档";
            case "12":
                return "已删除";
            case "13":
                return "已中止";
            case "14":
                return "领办逾期";
            case "15":
                return "反馈逾期";
            default:
                return "未知状态";
        }
    }

    // 督办主任方法
    public static String generateStatusStringForDirector(String status) {
        switch (status) {
            case "1":
                return "待交办";
            case "2":
                return "立项驳回";
            case "3":
                return "已交办";
            case "4":
                return "已领办";
            case "5":
            case "6":
            case "7":
                return "办理中";
            case "8":
                return "待审核";
            case "9":
                return "提交驳回";
            case "10":
                return "已办结";
            case "11":
                return "已归档";
            case "12":
                return "已删除";
            case "13":
                return "已中止";
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
        String[] strArray = str.split(",");
        Integer[] intArray = new Integer[strArray.length];
        for (int i = 0; i < strArray.length; i++) {
            intArray[i] = Integer.parseInt(strArray[i].trim());
        }
        return intArray;
    }




}
