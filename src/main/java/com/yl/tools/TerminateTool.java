package com.yl.tools;

import org.springframework.ai.tool.annotation.Tool;

/**
 * 终止工具类
 *
 * 功能说明：
 * 1. 用于在自主规划或多工具协作场景中，显式通知系统任务已经结束
 * 2. 当智能体已经完成全部任务，或者确认无法继续推进任务时，可以调用该工具
 * 3. 返回固定结果，作为任务终止标识
 *
 * 适用场景：
 * - 所有子任务已经执行完成
 * - 当前请求已满足，无需继续调用其他工具
 * - 因条件不足或执行受限，无法继续后续操作，需要结束流程
 */
public class TerminateTool {

    /**
     * 终止当前交互或任务流程
     *
     * 说明：
     * - 当任务已完成时，调用该方法结束工作
     * - 当助手无法继续推进当前任务时，也可调用该方法中断流程
     *
     * @return 任务结束提示信息
     */
    @Tool(description = """
            Terminate the interaction when the request is met OR if the assistant cannot proceed further with the task.
            When you have finished all the tasks, call this tool to end the work.
            """)
    public String doTerminate() {
        return "任务结束";
    }
}