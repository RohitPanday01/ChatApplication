package com.rohit.ChatApplication.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.BatchSize;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.parameters.P;

import java.sql.Time;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@BatchSize(size = 128)
@Table(name = "group_channel")
public class GroupChannel extends TimeStampBase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "group_id", columnDefinition = "uuid",updatable = false , nullable = false )
    private UUID groupId;

    @Version
    @Column(nullable = false, columnDefinition = "TIMESTAMP DEFAULT now()")
    private Instant version;

    @Column(name = "group_name", nullable = false)
    private String groupName = "";

    @OneToMany(mappedBy = "groupChannel", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<GroupInvitation> invitations = new HashSet<>();

    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL , orphanRemoval = true)
    private Set<GroupMember> memberSet = new HashSet<>();

//    @OneToMany(mappedBy = "group")
//    private Set<GroupMember> admins = new HashSet<>();
//
//    @OneToMany(mappedBy = "group")
//    private Set<GroupMember> bannedUsers = new HashSet<>();

    @OneToMany(mappedBy = "groupChannel" , cascade =  CascadeType.ALL , orphanRemoval = true)
    private List<GroupMessage> messages = new ArrayList<>();

    @OneToOne(fetch =  FetchType.LAZY)
    @JoinColumn(name = "last_message_id")
    private GroupMessage lastMessage;

    public Set<GroupMember> getAdmins(){
        return memberSet.stream()
                .filter(member->member.getGroupRole() == GroupRole.ADMIN && !member.isBanned())
                .collect(Collectors.toSet());

    }

    public Set<GroupMember> getBannedUsers() {
        return memberSet.stream()
                .filter(GroupMember::isBanned)
                .collect(Collectors.toSet());
    }

    public Set<GroupMember> getActiveMembers() {
        return memberSet.stream()
                .filter(member -> !member.isBanned())
                .collect(Collectors.toSet());
    }

    public GroupChannel(GroupMember creator){
        this.memberSet.add(creator);
        creator.setGroupRole(GroupRole.ADMIN);
    }
    
    public void checkIsAdmin(GroupMember  user) throws IllegalStateException{
        if(user.getGroupRole() != GroupRole.ADMIN){
            throw new IllegalStateException("User with id=%s is not a admin of this group".formatted(user.getUser().getUserId()));
        }
    }

    private void checkActionOnSameUser(GroupMember initiator, GroupMember target) throws IllegalStateException {
        if (initiator.equals(target))
            throw new IllegalStateException(
                    "user with id=%s cannot perform actions on itself !".formatted(initiator.getUser().getUserId()));
    }

    private void checkIsNotBanned(GroupMember user) throws IllegalStateException {
        if (user.isBanned())
            throw new IllegalStateException("user with id=%s has benn banned !".formatted(user.getUser().getUserId()));
    }
    

    public void  invite(GroupMember inviter , User invitee) throws IllegalStateException{
        if (!memberSet.contains(inviter)) {
            throw new IllegalStateException("Inviter is not a member of this GroupChannel");
        }

        boolean alreadyMember = memberSet.stream()
                .anyMatch(member -> member.getUser().equals(invitee));

        boolean alreadyInvited = invitations.stream()
                .anyMatch(invitation -> invitation.getUser().equals(invitee));

        if (alreadyMember || alreadyInvited) {
            throw new IllegalStateException("Invitee is already in this group or already invited");
        }

        checkNumOfMembers();

        GroupMessage invitationMessage = GroupMessage.invitation(this, inviter, invitee);
        messages.add(invitationMessage);

        GroupInvitation invitation = new GroupInvitation(invitee, this, invitationMessage);
        invitations.add(invitation);

        lastMessage = invitationMessage;
    }

    public void acceptInvitation(User invitee) throws IllegalStateException {
        GroupInvitation invitation = invitations.stream()
                .filter(inv -> inv.getUser().equals(invitee))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No invitation found!"));

//        GroupMember newMember = new GroupMember();
//        newMember.setGroupRole(GroupRole.Member);
//        newMember.setGroup(this);
//        newMember.setUser(invitee);

        GroupMember  newMember = new GroupMember(invitee , this, GroupRole.Member);

        invitations.remove(invitation);
        memberSet.add(newMember);
        checkNumOfMembers();

        GroupMessage joinMessage = GroupMessage.joinMessage(this, newMember);
        messages.add(joinMessage);
        lastMessage = joinMessage;
    }



    public void kickOff(GroupMember admin , GroupMember target) throws IllegalStateException{
        checkActionOnSameUser(admin ,target);
        checkIsAdmin(admin);

        if(getAdmins().contains(target)){
            throw new IllegalStateException("Cannot Kick Admin out");
        }
        if(!memberSet.contains(target)){
            throw new IllegalStateException("Member is Not Group");
        }
        memberSet.remove(target);
        GroupMessage kickOffMessage = GroupMessage.banMessage(this, admin,target);
        messages.add(kickOffMessage);
        lastMessage = kickOffMessage;
    }

    public void leave(GroupMember user) throws IllegalStateException {
        if (!memberSet.contains(user))
            throw new IllegalStateException("user is not in members of the channel !");
        if (getAdmins().contains(user) && getAdmins().size() == 1)
            throw new IllegalStateException("user is the last administrator of the channel !");

        if (memberSet.size() == 1) throw new IllegalStateException("user is the last member of the channel !");

        memberSet.remove(user);
        user.setGroup(null);
        GroupMessage leaveMessage = GroupMessage.leaveMessage(this, user, user);
        messages.add(leaveMessage);
        lastMessage = leaveMessage;
    }

    public void ban(GroupMember admin, GroupMember target) throws IllegalStateException {
        checkActionOnSameUser(admin, target);
        checkIsAdmin(admin);
        if (getAdmins().contains(target))
            throw new IllegalStateException("cannot editBanned target because it is an administrator !");

        target.setBanned(true);
        GroupMessage banMessage = GroupMessage.banMessage(this, admin, target);
        messages.add(banMessage);
        lastMessage = banMessage;
    }

    public void unban(GroupMember admin, GroupMember target) throws IllegalStateException {
        checkActionOnSameUser(admin, target);
        checkIsAdmin(admin);
        if (!getBannedUsers().contains(target)) throw new IllegalStateException("target user has not been banned !");

        target.setBanned(false);
        GroupMessage unbanMessage = GroupMessage.unbanMessage(this, admin, target);
        messages.add(unbanMessage);
        lastMessage = unbanMessage;
    }

    public void addToAdministrators(GroupMember admin, GroupMember target) throws IllegalStateException {
        checkActionOnSameUser(admin, target);
        checkIsAdmin(admin);
        if (!memberSet.contains(target))
            throw new IllegalStateException("target user is not in members of the channel !");
        checkIsNotBanned(target);

        target.setGroupRole(GroupRole.ADMIN);
    }

    public void removeFromAdministrators(GroupMember admin, GroupMember target) throws IllegalStateException {
        checkActionOnSameUser(admin, target);
        checkIsAdmin(admin);

        target.setGroupRole(GroupRole.Member);
    }

    public void addMessage(GroupMember from, String message) throws IllegalStateException {
        if (!memberSet.contains(from))
            throw new IllegalStateException("user is not in members of the channel !");
        checkIsNotBanned(from);

        GroupMessage groupMessage = new GroupMessage( from, message, this);
        messages.add(groupMessage);
        lastMessage = groupMessage;
    }


    void checkNumOfMembers() throws IllegalStateException {
        if (memberSet.isEmpty())
            throw new IllegalStateException("GroupChannel must contain at least 1 members !");
        else if (memberSet.size() > 200)
            throw new IllegalStateException("GroupChannel must contain at most 200 members !");
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof GroupChannel that)) return false;
        if (!super.equals(o)) return false;
        return Objects.equals(groupId, that.groupId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), groupId);
    }

    @Override
    public String toString() {
        return "GroupChannel{" +
                "updatedAt=" + updatedAt +
                ", createAt=" + createAt +
                ", groupId=" + groupId +
                ", memberSet=" + memberSet +
                '}';
    }
}