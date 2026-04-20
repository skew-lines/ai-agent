package com.yl.rag.rewrite;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.preretrieval.query.transformation.QueryTransformer;
import org.springframework.ai.rag.preretrieval.query.transformation.RewriteQueryTransformer;
import org.springframework.stereotype.Component;

/**
 * 查询重写器，调用LLM生成增强的Query
 */
@Component
public class QueryRewriter {

    private final QueryTransformer queryTransformer;

    public QueryRewriter(ChatModel dashscopeChatModel) {
        ChatClient.Builder builder = ChatClient.builder(dashscopeChatModel);
        // 创建查询重写转换器
        queryTransformer = RewriteQueryTransformer.builder()
                .chatClientBuilder(builder)
                .build();
    }

    /**
     * 执行查询重写
     *
     * @param prompt
     * @return
     */
    public String doQueryRewrite(String prompt) {
        Query query = new Query(prompt);
        // 执行查询重写
        Query transformedQuery = queryTransformer.transform(query);
        // 输出重写后的查询
        return transformedQuery.text();
    }


//    public void quickSort(int[] nums,int left, int right) {
//        if(left >= right) return;
//        int mid = partition(nums,left,right);
//        quickSort(nums,left,mid-1);
//        quickSort(nums,mid+1,right);
//    }
//    public int partition(int[] nums, int left, int right) {
//        int pivot = new Random().nextInt(right - left + 1) + left;
//        swap(nums,left,pivot);
//        int x = nums[left];
//        int i = left + 1;
//        int j = right;
//        while(true) {
//            while(i <= j && nums[i] < x) i++;
//            while(i <= j && nums[j] > x) j--;
//            if(i >= j) break;
//            swap(nums,i,j);
//            i++;
//            j--;
//        }
//        swap(nums,left,j);
//        return j;
//    }
//
//    public void swap(int[] nums, int i, int j) {
//        int tmp = nums[i];
//        nums[i] = nums[j];
//        nums[j] = tmp;
//    }
}
