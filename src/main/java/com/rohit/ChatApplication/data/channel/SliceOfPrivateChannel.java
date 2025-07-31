package com.rohit.ChatApplication.data.channel;


import com.rohit.ChatApplication.data.SliceList;
import com.rohit.ChatApplication.data.channel.profile.PrivateChannelProfile;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class SliceOfPrivateChannel {

    private int currentPage;
    private  int pageSize;
    private List<PrivateChannelProfile> channelProfiles;
    private boolean hasNext;


    public SliceOfPrivateChannel(SliceList<PrivateChannelProfile> sliceList) {
        this.currentPage = sliceList.getCurrentPage();
        this.pageSize = sliceList.getPageSize();
        this.hasNext = sliceList.isHasNext();
        this.channelProfiles = sliceList.getList();
    }

}
