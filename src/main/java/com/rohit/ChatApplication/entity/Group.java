package com.rohit.ChatApplication.entity;

import jakarta.persistence.*;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "chat_group")
public class Group {

    @Id
    @GeneratedValue
    @Column(name = "group_id", columnDefinition = "uuid",updatable = false , nullable = false  )
    private UUID groupId;

    @Column(name = "group_name", nullable = false)
    private String groupName;

    @Column(name = "group_link" )
    private String groupLink;

    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL)
    private Set<GroupMember> memberSet = new HashSet<>();


}
