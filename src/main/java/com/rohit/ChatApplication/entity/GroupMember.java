package com.rohit.ChatApplication.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "group_member")
@BatchSize(size = 64)
public class GroupMember extends TimeStampBase {

    @Version
    @Column(nullable = false, columnDefinition = "TIMESTAMP DEFAULT now()")
    private Instant version;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "group_members_id", columnDefinition = "uuid", nullable = false , updatable = false)
    private UUID groupMembersId;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne
    @JoinColumn(name = "group_id")
    private GroupChannel group;

    @Enumerated(EnumType.STRING)
    private GroupRole groupRole;

    private boolean isBanned = false;


    public GroupMember( User user , GroupChannel groupChannel , GroupRole groupRole){
        this.user = user;
        this.group = groupChannel;
        this.groupRole = groupRole;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof GroupMember that)) return false;
        if (!super.equals(o)) return false;
        return Objects.equals(groupMembersId, that.groupMembersId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), groupMembersId);
    }

    @Override
    public String toString() {
        return "GroupMember{" +
                "groupMembersId=" + groupMembersId +
                ", version=" + version +
                ", user=" + user +
                ", group=" + group +
                ", groupRole=" + groupRole +
                ", isBanned=" + isBanned +
                '}';
    }
}
