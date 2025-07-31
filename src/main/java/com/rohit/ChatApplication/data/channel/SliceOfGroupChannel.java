package com.rohit.ChatApplication.data.channel;


import com.rohit.ChatApplication.data.SliceList;
import com.rohit.ChatApplication.data.channel.profile.GroupChannelProfile;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class SliceOfGroupChannel {
    private int currentPage;
    private int pageSize;
    private boolean hasNext;
    private List<GroupChannelProfile> channels;

    public SliceOfGroupChannel(SliceList<GroupChannelProfile> sliceList) {
        this.currentPage = sliceList.getCurrentPage();
        this.pageSize = sliceList.getPageSize();
        this.hasNext = sliceList.isHasNext();
        this.channels = sliceList.getList();
    }
}