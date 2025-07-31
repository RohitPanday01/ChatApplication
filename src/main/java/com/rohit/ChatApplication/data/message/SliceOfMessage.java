package com.rohit.ChatApplication.data.message;

import com.rohit.ChatApplication.data.SliceList;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class SliceOfMessage <E extends  MessageDto>{
    private int currentPage;
    private int pageSize;
    private boolean hasNext;
    private List<E> messages;

    public SliceOfMessage(SliceList<E> sliceList) {
        this.currentPage = sliceList.getCurrentPage();
        this.pageSize = sliceList.getPageSize();
        this.hasNext = sliceList.isHasNext();
        this.messages = sliceList.getList();
    }
}
