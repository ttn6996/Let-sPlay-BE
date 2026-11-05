package com.letsplay.room;

import com.letsplay.auth.AuthService;
import com.letsplay.realtime.RoomRealtimeHub;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;

@Service
public class RoomService {
    private static final char[] CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();
    private final RoomRepository rooms;
    private final MembershipRepository memberships;
    private final AuthService auth;
    private final RoomProposalRepository proposals;
    private final RoomVoteRepository votes;
    private final RoomRealtimeHub realtime;
    private final RoomPushService push;
    private final RoomAvailabilitySlotRepository availabilitySlots;
    private final RoomAvailabilityVoteRepository availabilityVotes;
    private final SecureRandom random = new SecureRandom();

    public RoomService(RoomRepository rooms, MembershipRepository memberships, AuthService auth, RoomProposalRepository proposals, RoomVoteRepository votes, RoomRealtimeHub realtime, RoomPushService push, RoomAvailabilitySlotRepository availabilitySlots, RoomAvailabilityVoteRepository availabilityVotes) {
        this.rooms = rooms;
        this.memberships = memberships;
        this.auth = auth;
        this.proposals=proposals; this.votes=votes; this.realtime=realtime; this.push=push;
        this.availabilitySlots=availabilitySlots; this.availabilityVotes=availabilityVotes;
    }

    @Transactional
    public RoomResponse create(RoomRequests.CreateRoom request) {
        String requestId = request.requestId().toString();
        Room room = rooms.findByCreationRequestId(requestId).orElseGet(() -> {
            String code = newInviteCode();
            return rooms.save(new Room(UUID.randomUUID().toString(), request.roomName().trim(), code,
                    requestId, request.eventDate(), request.eventTime(), clean(request.region())));
        });
        return issueMembership(room, request.guestId(), request.displayName(), "HOST");
    }

    @Transactional
    public RoomResponse createForAccount(RoomRequests.CreateRoom request, String token) {
        String accountId=auth.accountId(token);
        String requestId=request.requestId().toString();
        Room room=rooms.findByCreationRequestId(requestId).orElseGet(() -> rooms.save(new Room(UUID.randomUUID().toString(),request.roomName().trim(),newInviteCode(),requestId,request.eventDate(),request.eventTime(),clean(request.region()))));
        return issueAccountMembership(room,accountId,request.displayName(),"HOST");
    }

    @Transactional
    public RoomResponse join(RoomRequests.JoinRoom request) {
        String inviteCode = request.inviteCode().trim().toUpperCase(Locale.ROOT);
        Room room = rooms.findByInviteCode(inviteCode)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Mã tham gia không hợp lệ."));
        if (!"OPEN".equals(room.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Phòng hiện không nhận thành viên mới.");
        }
        return issueMembership(room, request.guestId(), request.displayName(), "MEMBER");
    }

    @Transactional
    public RoomResponse joinForAccount(RoomRequests.JoinRoom request, String token) {
        String accountId=auth.accountId(token);
        String inviteCode=request.inviteCode().trim().toUpperCase(Locale.ROOT);
        Room room=rooms.findByInviteCode(inviteCode).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"Mã tham gia không hợp lệ."));
        if (!"OPEN".equals(room.getStatus())) throw new ResponseStatusException(HttpStatus.CONFLICT,"Phòng hiện không nhận thành viên mới.");
        return issueAccountMembership(room,accountId,request.displayName(),"MEMBER");
    }

    @Transactional(readOnly = true)
    public RoomPreview preview(RoomRequests.PreviewRoom request) {
        String inviteCode = request.inviteCode().trim().toUpperCase(Locale.ROOT);
        Room room = rooms.findByInviteCode(inviteCode)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Mã tham gia không hợp lệ."));
        if (!"OPEN".equals(room.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Phòng hiện không nhận thành viên mới.");
        }
        return RoomPreview.from(room,memberships.countByRoomId(room.getId()));
    }

    @Transactional(readOnly = true)
    public RoomResponse get(String roomId, String accessToken) {
        RoomAccess access = authorize(roomId, accessToken);
        return response(access.room(),access.member(),null);
    }

    @Transactional(readOnly = true)
    public RoomAccess authorize(String roomId, String accessToken) {
        Room room = rooms.findById(roomId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy phòng."));
        if (accessToken == null || accessToken.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Phiên truy cập không hợp lệ.");
        }
        Membership member = memberships.findByRoomIdAndAccessTokenHash(roomId, sha256(accessToken)).orElse(null);
        if(member==null) {
            try { member=memberships.findByRoomIdAndAccountId(roomId,auth.accountId(accessToken)).orElse(null); }
            catch(ResponseStatusException ignored) { }
        }
        if(member==null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Phiên đã hết hạn hoặc không hợp lệ.");
        return new RoomAccess(room, member);
    }

    @Transactional
    public void claim(String roomId,String accountToken,String guestToken) {
        String accountId=auth.accountId(accountToken);
        RoomAccess old=authorize(roomId,guestToken);
        if(old.member().getAccountId()!=null) return;
        if(memberships.findByRoomIdAndAccountId(roomId,accountId).isPresent()) throw new ResponseStatusException(HttpStatus.CONFLICT,"Tài khoản đã có quyền vào phòng này.");
        old.member().claim(accountId);
        changed(old.room());
    }

    @Transactional
    public RoomResponse addProposal(String roomId,String token,RoomRequests.CreateProposal request) {
        RoomAccess access=authorize(roomId,token);
        if(!java.util.Set.of("COLLECTING","VOTING").contains(access.room().getPhase())) throw new ResponseStatusException(HttpStatus.CONFLICT,"Phòng hiện không nhận đề xuất mới.");
        RoomProposal proposal=proposals.save(new RoomProposal(UUID.randomUUID().toString(),roomId,access.member().getId(),request.title().trim(),clean(request.description()),request.scheduledTime()));
        changed(access.room());
        String title=proposal.getTitle();
        afterCommit(()->push.notifyProposal(roomId,proposal.getId(),title));
        return response(access.room(),access.member(),null);
    }

    @Transactional
    public RoomResponse toggleVote(String roomId,String proposalId,String token) {
        RoomAccess access=authorize(roomId,token);
        if(!"VOTING".equals(access.room().getPhase())) throw new ResponseStatusException(HttpStatus.CONFLICT,"Phòng chưa ở giai đoạn bình chọn.");
        RoomProposal proposal=proposals.findById(proposalId).filter(p -> p.getRoomId().equals(roomId)).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"Không tìm thấy đề xuất."));
        votes.findByProposalIdAndMembershipId(proposal.getId(),access.member().getId()).ifPresentOrElse(
                vote -> votes.delete(vote),
                () -> votes.save(new RoomVote(UUID.randomUUID().toString(),proposal.getId(),access.member().getId())));
        changed(access.room());
        return response(access.room(),access.member(),null);
    }

    @Transactional
    public RoomResponse updateAvailabilityOptions(String roomId,String token,AvailabilityOptionsRequest request) {
        RoomAccess access=authorize(roomId,token);
        if(!"HOST".equals(access.member().getRole()))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Chỉ chủ phòng mới được thiết lập các khung giờ.");
        if("FINALIZED".equals(access.room().getPhase()))throw new ResponseStatusException(HttpStatus.CONFLICT,"Không thể đổi khung giờ sau khi đã chốt lịch.");
        var distinct=request.startsAt().stream().distinct().toList();
        if(distinct.size()!=request.startsAt().size())throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Không thể thêm trùng khung giờ.");
        availabilitySlots.deleteAllByRoomId(roomId);
        availabilitySlots.saveAll(distinct.stream().map(start->new RoomAvailabilitySlot(UUID.randomUUID().toString(),roomId,start)).toList());
        changed(access.room());
        return response(access.room(),access.member(),null);
    }

    @Transactional
    public RoomResponse updateAvailabilityVotes(String roomId,String token,AvailabilityVotesRequest request) {
        RoomAccess access=authorize(roomId,token);
        if("FINALIZED".equals(access.room().getPhase()))throw new ResponseStatusException(HttpStatus.CONFLICT,"Không thể đổi lựa chọn giờ sau khi đã chốt lịch.");
        var roomSlots=availabilitySlots.findAllByRoomIdOrderByStartsAtAsc(roomId);
        var validIds=roomSlots.stream().map(RoomAvailabilitySlot::getId).collect(java.util.stream.Collectors.toSet());
        var selected=request.slotIds().stream().collect(java.util.stream.Collectors.toSet());
        if(selected.size()!=request.slotIds().size()||!validIds.containsAll(selected))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Danh sách khung giờ không hợp lệ.");
        var ids=roomSlots.stream().map(RoomAvailabilitySlot::getId).toList();
        if(!ids.isEmpty())availabilityVotes.deleteAllBySlotIdIn(ids);
        availabilityVotes.saveAll(selected.stream().map(id->new RoomAvailabilityVote(UUID.randomUUID().toString(),id,access.member().getId())).toList());
        changed(access.room());
        return response(access.room(),access.member(),null);
    }

    @Transactional
    public RoomResponse selectAvailabilitySlot(String roomId,String slotId,String token) {
        RoomAccess access=authorize(roomId,token);
        if(!"HOST".equals(access.member().getRole()))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Chỉ chủ phòng mới được chốt giờ.");
        if("FINALIZED".equals(access.room().getPhase()))throw new ResponseStatusException(HttpStatus.CONFLICT,"Lịch đã được chốt.");
        RoomAvailabilitySlot slot=availabilitySlots.findById(slotId).filter(item->item.getRoomId().equals(roomId)).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Không tìm thấy khung giờ."));
        access.room().setEventDateTime(slot.getStartsAt());
        changed(access.room());
        return response(access.room(),access.member(),null);
    }

    @Transactional
    public RoomResponse changePhase(String roomId,String token,RoomRequests.ChangePhase request) {
        RoomAccess access=authorize(roomId,token);
        if(!"HOST".equals(access.member().getRole())) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Chỉ chủ phòng mới được đổi giai đoạn.");
        String phase=request.phase().trim().toUpperCase(Locale.ROOT);
        if(!java.util.Set.of("COLLECTING","VOTING","FINALIZED").contains(phase)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Giai đoạn không hợp lệ.");
        String selected=null;
        if(request.selectedProposalId()!=null&&!request.selectedProposalId().isBlank()) selected=proposals.findById(request.selectedProposalId()).filter(p -> p.getRoomId().equals(roomId)).map(RoomProposal::getId).orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,"Đề xuất được chọn không thuộc phòng này."));
        if("FINALIZED".equals(phase)&&selected==null) selected=proposals.findAllByRoomIdOrderByCreatedAtAsc(roomId).stream().max(java.util.Comparator.comparingLong(p->votes.countByProposalId(p.getId()))).map(RoomProposal::getId).orElse(null);
        String currentPhase=access.room().getPhase().trim().toUpperCase(Locale.ROOT);
        boolean allowed=switch(currentPhase) {
            case "IDEAS","COLLECTING" -> "COLLECTING".equals(phase)||"VOTING".equals(phase);
            case "VOTING" -> "COLLECTING".equals(phase)||"VOTING".equals(phase)||"FINALIZED".equals(phase);
            case "FINALIZED" -> "FINALIZED".equals(phase);
            default -> false;
        };
        if(!allowed) throw new ResponseStatusException(HttpStatus.CONFLICT,"Không thể chuyển sang giai đoạn đã chọn.");
        // Keep existing votes when reopening contributions. Members can add proposals during voting,
        // so returning to collect more ideas must not erase any group's work.
        access.room().changePhase(phase,selected); changed(access.room());
        return response(access.room(),access.member(),null);
    }

    @Transactional
    public void leave(String roomId,String token) {
        RoomAccess access=authorize(roomId,token);
        Membership leaving=access.member(); Room room=access.room();
        if("HOST".equals(leaving.getRole())) {
            Membership next=memberships.findAllByRoomIdAndIdNotOrderByCreatedAtAsc(roomId,leaving.getId()).stream().findFirst().orElse(null);
            if(next==null){rooms.delete(room);afterCommit(()->realtime.closeRoom(roomId));return;}
            next.promoteToHost();
        }
        memberships.delete(leaving); changed(room); afterCommit(()->realtime.disconnectMembership(roomId,leaving.getId()));
    }

    @Transactional
    public RoomResponse updateProposal(String roomId,String proposalId,String token,RoomRequests.CreateProposal request){
        RoomAccess access=authorize(roomId,token); Room room=access.room();
        RoomProposal proposal=proposalInRoom(roomId,proposalId);
        ensureProposalEditable(room);
        ensureCanManageProposal(access.member(),proposal);
        proposal.update(request.title().trim(),clean(request.description()),request.scheduledTime());
        changed(room);return response(room,access.member(),null);
    }

    @Transactional
    public RoomResponse deleteProposal(String roomId,String proposalId,String token){
        RoomAccess access=authorize(roomId,token); Room room=access.room();
        RoomProposal proposal=proposalInRoom(roomId,proposalId);
        ensureProposalEditable(room);ensureCanManageProposal(access.member(),proposal);
        votes.deleteAllByProposalId(proposalId);proposals.delete(proposal);
        if(proposalId.equals(room.getSelectedProposalId()))room.changePhase(room.getPhase(),null);
        changed(room);return response(room,access.member(),null);
    }

    @Transactional
    public RoomResponse setProposalCompleted(String roomId,String proposalId,String token,boolean completed){
        RoomAccess access=authorize(roomId,token); RoomProposal proposal=proposalInRoom(roomId,proposalId);
        proposal.setCompleted(completed); changed(access.room());
        return response(access.room(),access.member(),null);
    }

    @Transactional
    public void kickMember(String roomId,String membershipId,String token){
        RoomAccess access=authorize(roomId,token);
        if(!"HOST".equals(access.member().getRole()))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Chỉ chủ phòng mới được mời thành viên ra khỏi phòng.");
        if(access.member().getId().equals(membershipId))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Chủ phòng không thể tự mời mình ra khỏi phòng.");
        Membership target=memberships.findById(membershipId).filter(m->m.getRoomId().equals(roomId)).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Không tìm thấy thành viên."));
        votes.deleteAllByMembershipId(membershipId);
        memberships.delete(target);changed(access.room());afterCommit(()->realtime.disconnectMembership(roomId,membershipId));
    }

    @Transactional(readOnly=true)
    public java.util.List<MyRoomSummary> myRooms(String accountToken,java.util.List<String> guestTokens){
        var found=new java.util.LinkedHashMap<String,Membership>();
        if(accountToken!=null&&!accountToken.isBlank()){
            String accountId=auth.accountId(accountToken);
            memberships.findAllByAccountIdOrderByCreatedAtDesc(accountId).forEach(m->found.put(m.getId(),m));
        }
        if(guestTokens!=null)for(String token:guestTokens)if(token!=null&&!token.isBlank())memberships.findByAccessTokenHash(sha256(token)).ifPresent(m->found.put(m.getId(),m));
        return found.values().stream().map(m->rooms.findById(m.getRoomId()).map(r->new MyRoomSummary(r.getId(),r.getName(),r.getInviteCode(),r.getPhase(),m.getRole(),(int)memberships.countByRoomId(r.getId()),r.getRevision(),r.getUpdatedAt())).orElse(null)).filter(java.util.Objects::nonNull).sorted(java.util.Comparator.comparing(MyRoomSummary::updatedAt).reversed()).toList();
    }

    private RoomProposal proposalInRoom(String roomId,String proposalId){return proposals.findById(proposalId).filter(p->p.getRoomId().equals(roomId)).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Không tìm thấy đề xuất."));}
    private static void ensureProposalEditable(Room room){if("FINALIZED".equals(room.getPhase()))throw new ResponseStatusException(HttpStatus.CONFLICT,"Không thể sửa đề xuất sau khi đã chốt lịch.");}
    private static void ensureCanManageProposal(Membership member,RoomProposal proposal){if(!"HOST".equals(member.getRole())&&!member.getId().equals(proposal.getCreatedByMembershipId()))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Chỉ người tạo đề xuất hoặc chủ phòng mới được chỉnh sửa.");}
    private void afterCommit(Runnable callback){if(TransactionSynchronizationManager.isSynchronizationActive())TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization(){@Override public void afterCommit(){callback.run();}});else callback.run();}
    public record MyRoomSummary(String roomId,String name,String inviteCode,String phase,String viewerRole,int memberCount,long revision,java.time.Instant updatedAt){}

    private RoomResponse issueAccountMembership(Room room,String accountId,String displayName,String role) {
        Membership member=memberships.findByRoomIdAndAccountId(room.getId(),accountId).orElse(null);
        if(member==null) { member=memberships.save(new Membership(UUID.randomUUID().toString(),room.getId(),accountId,displayName.trim(),role,true)); if("MEMBER".equals(role)) changed(room); }
        return response(room,member,null);
    }

    private RoomResponse issueMembership(Room room, UUID guestId, String displayName, String role) {
        String token = randomToken();
        String guestIdHash = sha256(guestId.toString());
        Membership member = memberships.findByRoomIdAndGuestIdHash(room.getId(), guestIdHash).orElse(null);
        if (member == null) {
            member = memberships.save(new Membership(UUID.randomUUID().toString(), room.getId(),
                    guestIdHash, sha256(token), displayName.trim(), role));
            if ("MEMBER".equals(role)) changed(room);
        } else {
            boolean displayNameChanged=!member.getDisplayName().equals(displayName.trim());
            member.refreshSession(sha256(token), displayName.trim());
            if(displayNameChanged)changed(room);
        }
        return response(room,member,token);
    }

    private RoomResponse response(Room room,Membership member,String token) {
        var items=proposals.findAllByRoomIdOrderByCreatedAtAsc(room.getId());
        var itemIds=items.stream().map(RoomProposal::getId).toList();
        var allVotes=itemIds.isEmpty()?java.util.List.<RoomVote>of():votes.findAllByProposalIdIn(itemIds);
        var voterIds=allVotes.stream().map(RoomVote::getMembershipId).distinct().toList();
        var members=memberships.findAllById(voterIds).stream().collect(java.util.stream.Collectors.toMap(Membership::getId,m -> m));
        var votesByProposal=allVotes.stream().collect(java.util.stream.Collectors.groupingBy(RoomVote::getProposalId));
        var views=items.stream().map(p -> {
            var proposalVotes=votesByProposal.getOrDefault(p.getId(),java.util.List.of());
            var voters=proposalVotes.stream().map(v -> new RoomResponse.Voter(v.getMembershipId().equals(member.getId())?"Bạn":"Ẩn danh")).toList();
            return new RoomResponse.Proposal(p.getId(),member.getId().equals(p.getCreatedByMembershipId()),p.getTitle(),p.getDescription(),p.getScheduledTime(),p.getCreatedAt(),proposalVotes.size(),voters,proposalVotes.stream().anyMatch(v -> v.getMembershipId().equals(member.getId())),p.isCompleted());
        }).toList();
        var onlineIds=realtime.onlineMembershipIds(room.getId());
        var allMembers=memberships.findAllByRoomIdOrderByCreatedAtAsc(room.getId());
        var roomMembers=java.util.stream.IntStream.range(0,allMembers.size()).mapToObj(index -> {
            Membership listed=allMembers.get(index);
            String displayName=listed.getId().equals(member.getId())?listed.getDisplayName():"Thành viên "+(index+1);
            return new RoomResponse.Member(listed.getId(),displayName,listed.getRole(),onlineIds.contains(listed.getId()));
        }).toList();
        var viewer=roomMembers.stream().filter(m -> m.id().equals(member.getId())).findFirst().orElse(new RoomResponse.Member(member.getId(),member.getDisplayName(),member.getRole(),onlineIds.contains(member.getId())));
        var slots=availabilitySlots.findAllByRoomIdOrderByStartsAtAsc(room.getId());
        var slotIds=slots.stream().map(RoomAvailabilitySlot::getId).toList();
        var slotVotes=slotIds.isEmpty()?java.util.List.<RoomAvailabilityVote>of():availabilityVotes.findAllBySlotIdIn(slotIds);
        var voteCounts=slotVotes.stream().collect(java.util.stream.Collectors.groupingBy(RoomAvailabilityVote::getSlotId,java.util.stream.Collectors.counting()));
        var slotsView=slots.stream().map(slot->new RoomResponse.AvailabilitySlot(slot.getId(),slot.getStartsAt(),voteCounts.getOrDefault(slot.getId(),0L),slotVotes.stream().anyMatch(v->v.getSlotId().equals(slot.getId())&&v.getMembershipId().equals(member.getId())),room.getEventDate()!=null&&room.getEventTime()!=null&&room.getEventDate().atTime(room.getEventTime()).equals(slot.getStartsAt()))).toList();
        return new RoomResponse(room.getId(),room.getName(),room.getInviteCode(),room.getPhase(),room.getStatus(),room.getEventDate(),room.getEventTime(),room.getRegion(),room.getCreatedAt(),room.getRevision(),memberships.countByRoomId(room.getId()),viewer,roomMembers,token,room.getSelectedProposalId(),views,slotsView);
    }

    private void changed(Room room) {
        room.bumpRevision();
        String roomId=room.getId(); long revision=room.getRevision();
        if(TransactionSynchronizationManager.isSynchronizationActive()) TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCommit(){realtime.publishRevision(roomId,revision);}
        });
        else realtime.publishRevision(roomId,revision);
    }

    private String newInviteCode() {
        for (int attempt = 0; attempt < 10; attempt++) {
            StringBuilder code = new StringBuilder(6);
            for (int i = 0; i < 6; i++) code.append(CODE_ALPHABET[random.nextInt(CODE_ALPHABET.length)]);
            String candidate = code.toString();
            if (!rooms.existsByInviteCode(candidate)) return candidate;
        }
        throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Không thể tạo mã tham gia lúc này.");
    }

    private String randomToken() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private static String clean(String value) { return value == null || value.isBlank() ? null : value.trim(); }
}
