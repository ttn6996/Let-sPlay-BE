package com.letsplay.room;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@Service
public class RoomFeatureService {
    private final JdbcTemplate db;
    private final RoomService rooms;
    private final RoomPushService push;
    public RoomFeatureService(JdbcTemplate db, RoomService rooms,RoomPushService push) { this.db=db; this.rooms=rooms; this.push=push; }

    @Transactional
    public RoomResponse updatePlan(String roomId,String token,RoomFeatureRequests.Plan request) {
        RoomAccess access=rooms.authorize(roomId,token); Room room=access.room();
        if(!"HOST".equals(access.member().getRole()))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Chỉ chủ phòng mới được sửa kế hoạch.");
        String before=planSnapshot(room);
        room.updatePlan(request.name().trim(),request.eventDate(),request.eventTime(),clean(request.region()),clean(request.notes()),request.latitude(),request.longitude(),clean(request.weatherFallback()));
        room.setStatus(request.cancelled()?"CANCELLED":"OPEN"); rooms.markRoomChanged(room);
        if(!request.cancelled()&&"FINALIZED".equals(room.getPhase()))room.changePhase("COLLECTING",null);
        String summary="Trước: "+before+" → Sau: "+planSnapshot(room)+(request.cancelled()?" · đã hủy":"");if(summary.length()>2000)summary=summary.substring(0,1997)+"…";
        db.update("INSERT INTO room_plan_history(id,room_id,actor_membership_id,actor_display_name,change_summary) VALUES (?,?,?,?,?)",UUID.randomUUID().toString(),roomId,access.member().getId(),access.member().getDisplayName(),summary);
        String title=request.cancelled()?"Kế hoạch đã bị hủy":"Kế hoạch vừa được cập nhật";
        notifyRoom(roomId,access.member().getId(),request.cancelled()?"PLAN_CANCELLED":"PLAN_UPDATED",title,room.getName());
        afterCommit(()->push.notifyRoom(roomId,title,room.getName()));
        return rooms.get(roomId,token);
    }

    @Transactional
    public RoomResponse setAttendance(String roomId,String token,String status) {
        RoomAccess access=rooms.authorize(roomId,token); String normalized=status.trim().toUpperCase(Locale.ROOT);
        if(!Set.of("GOING","MAYBE","NOT_GOING","ON_THE_WAY","LATE","ARRIVED").contains(normalized)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Trạng thái tham gia không hợp lệ.");
        db.update("INSERT INTO room_attendance(room_id,membership_id,attendance_status,updated_at) VALUES (?,?,?,now()) ON CONFLICT(room_id,membership_id) DO UPDATE SET attendance_status=EXCLUDED.attendance_status,updated_at=now()",roomId,access.member().getId(),normalized);
        rooms.markRoomChanged(access.room()); return rooms.get(roomId,token);
    }

    @Transactional
    public void setMeetupPoint(String roomId,String token,Double lat,Double lon) {
        RoomAccess access=rooms.authorize(roomId,token); if(lat!=null&&(lat < -90||lat > 90)||lon!=null&&(lon < -180||lon > 180))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Tọa độ không hợp lệ.");
        access.room().setMeetupPoint(lat,lon); rooms.markRoomChanged(access.room());
    }

    @Transactional
    public void savePreferences(String roomId,String token,RoomFeatureRequests.Preferences req) {
        RoomAccess a=rooms.authorize(roomId,token); if(req.budgetMin()!=null&&req.budgetMax()!=null&&req.budgetMin()>req.budgetMax())throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Ngân sách tối thiểu không thể lớn hơn tối đa.");
        db.update("INSERT INTO room_member_preferences(room_id,membership_id,preferences,budget_min,budget_max,updated_at) VALUES (?,?,?,?,?,now()) ON CONFLICT(room_id,membership_id) DO UPDATE SET preferences=EXCLUDED.preferences,budget_min=EXCLUDED.budget_min,budget_max=EXCLUDED.budget_max,updated_at=now()",roomId,a.member().getId(),clean(req.preferences()),req.budgetMin(),req.budgetMax());
        rooms.markRoomChanged(a.room());
    }

    @Transactional
    public void saveRide(String roomId,String token,RoomFeatureRequests.Ride req) {
        RoomAccess a=rooms.authorize(roomId,token); if(!req.hasVehicle()&&req.seatsAvailable()>0)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Chỉ khai báo chỗ ngồi nếu bạn có xe.");
        db.update("INSERT INTO room_rides(room_id,membership_id,has_vehicle,seats_available,pickup_area,note,updated_at) VALUES (?,?,?,?,?,?,now()) ON CONFLICT(room_id,membership_id) DO UPDATE SET has_vehicle=EXCLUDED.has_vehicle,seats_available=EXCLUDED.seats_available,pickup_area=EXCLUDED.pickup_area,note=EXCLUDED.note,updated_at=now()",roomId,a.member().getId(),req.hasVehicle(),req.seatsAvailable(),clean(req.pickupArea()),clean(req.note()));
        rooms.markRoomChanged(a.room());
    }

    @Transactional(readOnly=true)
    public Map<String,Object> collaboration(String roomId,String token) {
        RoomAccess a=rooms.authorize(roomId,token); String mid=a.member().getId();
        var attendance=db.query("SELECT m.id,m.display_name,COALESCE(x.attendance_status,'NO_RESPONSE') status FROM room_memberships m LEFT JOIN room_attendance x ON x.room_id=m.room_id AND x.membership_id=m.id WHERE m.room_id=? ORDER BY m.created_at",(rs,n)->Map.of("membershipId",rs.getString(1),"displayName",rs.getString(2),"status",rs.getString(3)),roomId);
        var preferences=db.query("SELECT membership_id,preferences,budget_min,budget_max FROM room_member_preferences WHERE room_id=?",(rs,n)->Map.of("membershipId",rs.getString(1),"preferences",Objects.toString(rs.getString(2),""),"budgetMin",rs.getObject(3)==null?0L:rs.getLong(3),"budgetMax",rs.getObject(4)==null?0L:rs.getLong(4)),roomId);
        var rides=db.query("SELECT m.id,m.display_name,r.membership_id IS NOT NULL,r.has_vehicle,r.seats_available,r.pickup_area,r.note FROM room_memberships m LEFT JOIN room_rides r ON r.room_id=m.room_id AND r.membership_id=m.id WHERE m.room_id=? ORDER BY m.created_at",(rs,n)->Map.of("membershipId",rs.getString(1),"displayName",rs.getString(2),"registered",rs.getBoolean(3),"hasVehicle",rs.getObject(4)==null?false:rs.getBoolean(4),"seatsAvailable",rs.getObject(5)==null?0:rs.getInt(5),"pickupArea",Objects.toString(rs.getString(6),""),"note",Objects.toString(rs.getString(7),"")),roomId);
        var notifications=db.query("SELECT id,notification_type,title,body,created_at,read_at FROM room_notifications WHERE room_id=? AND membership_id=? ORDER BY created_at DESC LIMIT 50",(rs,n)->Map.of("id",rs.getString(1),"type",rs.getString(2),"title",rs.getString(3),"body",rs.getString(4),"createdAt",rs.getTimestamp(5).toInstant().toString(),"read",rs.getTimestamp(6)!=null),roomId,mid);
        var history=db.query("SELECT h.id,h.actor_display_name,h.change_summary,h.created_at FROM room_plan_history h WHERE h.room_id=? ORDER BY h.created_at DESC LIMIT 25",(rs,n)->Map.of("id",rs.getString(1),"actor",rs.getString(2),"summary",rs.getString(3),"createdAt",rs.getTimestamp(4).toInstant().toString()),roomId);
        return Map.of("attendance",attendance,"preferences",preferences,"rides",rides,"notifications",notifications,"planHistory",history);
    }

    @Transactional
    public void markNotificationsRead(String roomId,String token) { RoomAccess a=rooms.authorize(roomId,token); db.update("UPDATE room_notifications SET read_at=now() WHERE room_id=? AND membership_id=? AND read_at IS NULL",roomId,a.member().getId()); }

    @Transactional(readOnly=true)
    public Map<String,Object> calendarLink(String roomId,String token){RoomAccess a=rooms.authorize(roomId,token);String id=db.query("SELECT google_event_id FROM room_calendar_links WHERE room_id=? AND membership_id=?",rs->rs.next()?rs.getString(1):null,roomId,a.member().getId());return Collections.singletonMap("eventId",id);}
    @Transactional
    public void saveCalendarLink(String roomId,String token,String eventId){RoomAccess a=rooms.authorize(roomId,token);db.update("INSERT INTO room_calendar_links(room_id,membership_id,google_event_id,updated_at) VALUES (?,?,?,now()) ON CONFLICT(room_id,membership_id) DO UPDATE SET google_event_id=EXCLUDED.google_event_id,updated_at=now()",roomId,a.member().getId(),eventId);}

    @Transactional
    public Map<String,Object> saveExpense(String roomId,String expenseId,String token,RoomFeatureRequests.Expense req) {
        RoomAccess a=rooms.authorize(roomId,token); validateAllocations(roomId,req.totalAmount(),req.payers(),true,expenseId); validateAllocations(roomId,req.totalAmount(),req.shares(),false,expenseId);
        boolean create=expenseId==null; String id=create?UUID.randomUUID().toString():expenseId;
        String before=create?"":expenseSnapshot(roomId,id);
        if(create) db.update("INSERT INTO room_expenses(id,room_id,title,total_amount,note,created_by_membership_id,created_by_display_name) VALUES (?,?,?,?,?,?,?)",id,roomId,req.title().trim(),req.totalAmount(),clean(req.note()),a.member().getId(),a.member().getDisplayName());
        else { int count=db.update("UPDATE room_expenses SET title=?,total_amount=?,note=?,updated_at=now() WHERE id=? AND room_id=?",req.title().trim(),req.totalAmount(),clean(req.note()),id,roomId); if(count==0)throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Không tìm thấy khoản chi."); }
        db.update("DELETE FROM room_expense_payers WHERE expense_id=?",id); db.update("DELETE FROM room_expense_shares WHERE expense_id=?",id);
        req.payers().forEach(x->db.update("INSERT INTO room_expense_payers(expense_id,membership_id,display_name,amount) VALUES (?,?,?,?)",id,x.membershipId(),memberName(roomId,x.membershipId()),x.amount()));
        req.shares().forEach(x->db.update("INSERT INTO room_expense_shares(expense_id,membership_id,display_name,amount) VALUES (?,?,?,?)",id,x.membershipId(),memberName(roomId,x.membershipId()),x.amount()));
        String after=expenseSnapshot(roomId,id);
        audit(roomId,id,a.member().getId(),create?"CREATE":"UPDATE",(create?"Tạo khoản chi: ":"Trước: "+Objects.toString(before,"không rõ")+" → Sau: ")+Objects.toString(after,req.title()+" · "+req.totalAmount()+" ₫"));
        notifyRoom(roomId,a.member().getId(),"EXPENSE_UPDATED",create?"Khoản chi mới":"Khoản chi đã cập nhật",req.title().trim());
        afterCommit(()->push.notifyRoom(roomId,create?"Khoản chi mới":"Khoản chi đã cập nhật",req.title().trim()));
        rooms.markRoomChanged(a.room()); return expense(roomId,id);
    }

    @Transactional
    public void deleteExpense(String roomId,String id,String token) { RoomAccess a=rooms.authorize(roomId,token); String title=db.query("SELECT title FROM room_expenses WHERE id=? AND room_id=?",rs->rs.next()?rs.getString(1):null,id,roomId);if(title==null)throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Không tìm thấy khoản chi."); audit(roomId,id,a.member().getId(),"DELETE",title);db.update("DELETE FROM room_expenses WHERE id=? AND room_id=?",id,roomId);rooms.markRoomChanged(a.room()); }

    @Transactional(readOnly=true)
    public Map<String,Object> expenses(String roomId,String token) {
        rooms.authorize(roomId,token); var ids=db.queryForList("SELECT id FROM room_expenses WHERE room_id=? ORDER BY created_at DESC",String.class,roomId);var list=ids.stream().map(id->expense(roomId,id)).toList();
        var audit=db.query("SELECT a.id,a.expense_id,a.actor_display_name,a.action,a.description,a.created_at FROM room_expense_audit a WHERE a.room_id=? ORDER BY a.created_at DESC LIMIT 100",(rs,n)->Map.of("id",rs.getString(1),"expenseId",Objects.toString(rs.getString(2),""),"actor",rs.getString(3),"action",rs.getString(4),"description",rs.getString(5),"createdAt",rs.getTimestamp(6).toInstant().toString()),roomId);
        return Map.of("expenses",list,"audit",audit,"balances",balances(roomId));
    }

    @Transactional
    public Map<String,Object> setSettlement(String roomId,String settlementId,String token,String status) { RoomAccess a=rooms.authorize(roomId,token);String state=status.toUpperCase(Locale.ROOT);if(!Set.of("PAID","PENDING").contains(state))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Trạng thái thanh toán không hợp lệ.");int n=db.update("UPDATE room_expense_settlements SET status=?,paid_at=CASE WHEN ?='PAID' THEN now() ELSE NULL END WHERE id=? AND room_id=?",state,state,settlementId,roomId);if(n==0)throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Không tìm thấy khoản cần thanh toán.");rooms.markRoomChanged(a.room());return expenses(roomId,token); }

    @Transactional
    public Map<String,Object> createSettlement(String roomId,String token,RoomFeatureRequests.Settlement request) { RoomAccess a=rooms.authorize(roomId,token);db.queryForObject("SELECT id FROM rooms WHERE id=? FOR UPDATE",String.class,roomId);if(request.fromMembershipId().equals(request.toMembershipId()))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Người chuyển và người nhận phải khác nhau.");validateFinancialParticipant(roomId,request.fromMembershipId());validateFinancialParticipant(roomId,request.toMembershipId());@SuppressWarnings("unchecked") var suggestions=(List<Map<String,Object>>)balances(roomId).get("suggestedTransfers");long allowed=suggestions.stream().filter(x->request.fromMembershipId().equals(x.get("fromMembershipId"))&&request.toMembershipId().equals(x.get("toMembershipId"))).mapToLong(x->((Number)x.get("amount")).longValue()).findFirst().orElse(0);if(request.amount()>allowed)throw new ResponseStatusException(HttpStatus.CONFLICT,"Số tiền đã vượt khoản đang cần thanh toán; hãy tải lại số dư.");String id=UUID.randomUUID().toString();db.update("INSERT INTO room_expense_settlements(id,room_id,from_membership_id,from_display_name,to_membership_id,to_display_name,amount,status,created_by_membership_id,created_by_display_name,paid_at) VALUES (?,?,?,?,?,?,?,'PAID',?,?,now())",id,roomId,request.fromMembershipId(),participantName(roomId,request.fromMembershipId()),request.toMembershipId(),participantName(roomId,request.toMembershipId()),request.amount(),a.member().getId(),a.member().getDisplayName());audit(roomId,null,a.member().getId(),"SETTLEMENT",participantName(roomId,request.fromMembershipId())+" đã chuyển "+request.amount()+" ₫ cho "+participantName(roomId,request.toMembershipId()));rooms.markRoomChanged(a.room());return expenses(roomId,token); }

    private Map<String,Object> expense(String roomId,String id) {
        var row=db.queryForMap("SELECT id,title,total_amount,note,created_at,updated_at FROM room_expenses WHERE id=? AND room_id=?",id,roomId);
        var payers=db.query("SELECT p.membership_id,COALESCE(m.display_name,p.display_name),p.amount FROM room_expense_payers p LEFT JOIN room_memberships m ON m.id=p.membership_id WHERE p.expense_id=? ORDER BY p.display_name",(rs,n)->Map.of("membershipId",rs.getString(1),"displayName",rs.getString(2),"amount",rs.getLong(3)),id);
        var shares=db.query("SELECT p.membership_id,COALESCE(m.display_name,p.display_name),p.amount FROM room_expense_shares p LEFT JOIN room_memberships m ON m.id=p.membership_id WHERE p.expense_id=? ORDER BY p.display_name",(rs,n)->Map.of("membershipId",rs.getString(1),"displayName",rs.getString(2),"amount",rs.getLong(3)),id);
        return Map.of("id",row.get("id"),"title",row.get("title"),"totalAmount",row.get("total_amount"),"note",Objects.toString(row.get("note"),""),"createdAt",row.get("created_at").toString(),"updatedAt",row.get("updated_at").toString(),"payers",payers,"shares",shares);
    }
    private Map<String,Object> balances(String roomId) {
        var rows=db.query("SELECT x.membership_id,x.display_name,SUM(x.delta) balance FROM (SELECT p.membership_id,p.display_name,p.amount delta FROM room_expense_payers p JOIN room_expenses e ON e.id=p.expense_id WHERE e.room_id=? UNION ALL SELECT s.membership_id,s.display_name,-s.amount FROM room_expense_shares s JOIN room_expenses e ON e.id=s.expense_id WHERE e.room_id=?) x GROUP BY x.membership_id,x.display_name ORDER BY x.display_name",(rs,n)->new Balance(rs.getString(1),rs.getString(2),rs.getLong(3)),roomId,roomId);
        Map<String,Long> adjustment=new HashMap<>();db.query("SELECT from_membership_id,to_membership_id,amount FROM room_expense_settlements WHERE room_id=? AND status='PAID'",rs->{while(rs.next()){String from=rs.getString(1),to=rs.getString(2);long amount=rs.getLong(3);adjustment.merge(from,amount,Long::sum);adjustment.merge(to,-amount,Long::sum);}},roomId);
        rows=rows.stream().map(b->new Balance(b.id(),b.name(),b.balance()+adjustment.getOrDefault(b.id(),0L))).toList();
        List<Balance> creditors=new ArrayList<>(),debtors=new ArrayList<>();for(Balance b:rows)if(b.balance()>0)creditors.add(b);else if(b.balance()<0)debtors.add(new Balance(b.id(),b.name(),-b.balance()));
        List<Map<String,Object>> suggested=new ArrayList<>();int i=0,j=0;while(i<debtors.size()&&j<creditors.size()){Balance d=debtors.get(i),c=creditors.get(j);long amount=Math.min(d.balance(),c.balance());suggested.add(Map.of("fromMembershipId",d.id(),"from",d.name(),"toMembershipId",c.id(),"to",c.name(),"amount",amount));debtors.set(i,new Balance(d.id(),d.name(),d.balance()-amount));creditors.set(j,new Balance(c.id(),c.name(),c.balance()-amount));if(debtors.get(i).balance()==0)i++;if(creditors.get(j).balance()==0)j++;}
        var settlements=db.query("SELECT s.id,s.from_membership_id,s.from_display_name,s.to_membership_id,s.to_display_name,s.amount,s.status FROM room_expense_settlements s WHERE s.room_id=? ORDER BY s.created_at DESC",(rs,n)->Map.of("id",rs.getString(1),"fromMembershipId",rs.getString(2),"from",rs.getString(3),"toMembershipId",rs.getString(4),"to",rs.getString(5),"amount",rs.getLong(6),"status",rs.getString(7)),roomId);
        return Map.of("members",rows.stream().map(b->Map.of("membershipId",b.id(),"displayName",b.name(),"netAmount",b.balance())).toList(),"suggestedTransfers",suggested,"settlements",settlements);
    }
    private void validateAllocations(String roomId,long total,List<RoomFeatureRequests.Allocation> items,boolean positive,String expenseId) { if(items.stream().mapToLong(RoomFeatureRequests.Allocation::amount).sum()!=total)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Tổng các phần phải bằng tổng khoản chi.");Set<String> seen=new HashSet<>();for(var item:items){if(positive&&item.amount()==0)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Người thanh toán cần có số tiền lớn hơn 0.");if(!seen.add(item.membershipId()))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Thành viên bị lặp trong phân bổ.");Integer valid=db.queryForObject("SELECT COUNT(*) FROM room_memberships WHERE id=? AND room_id=?",Integer.class,item.membershipId(),roomId);if((valid==null||valid==0)&&expenseId!=null){Integer existing=db.queryForObject("SELECT (SELECT COUNT(*) FROM room_expense_payers WHERE expense_id=? AND membership_id=?)+(SELECT COUNT(*) FROM room_expense_shares WHERE expense_id=? AND membership_id=?)",Integer.class,expenseId,item.membershipId(),expenseId,item.membershipId());valid=existing;}if(valid==null||valid==0)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Có thành viên không thuộc khoản chi hiện tại.");} }
    private void validateMembership(String roomId,String membershipId){Integer valid=db.queryForObject("SELECT COUNT(*) FROM room_memberships WHERE id=? AND room_id=?",Integer.class,membershipId,roomId);if(valid==null||valid==0)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Có thành viên không thuộc phòng.");}
    private void validateFinancialParticipant(String roomId,String membershipId){Integer valid=db.queryForObject("SELECT (SELECT COUNT(*) FROM room_memberships WHERE room_id=? AND id=?)+(SELECT COUNT(*) FROM room_expense_payers p JOIN room_expenses e ON e.id=p.expense_id WHERE e.room_id=? AND p.membership_id=?)+(SELECT COUNT(*) FROM room_expense_shares s JOIN room_expenses e ON e.id=s.expense_id WHERE e.room_id=? AND s.membership_id=?)",Integer.class,roomId,membershipId,roomId,membershipId,roomId,membershipId);if(valid==null||valid==0)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Người này không tham gia sổ chi tiêu của phòng.");}
    private void notifyRoom(String roomId,String actor,String type,String title,String body){db.update("INSERT INTO room_notifications(id,room_id,membership_id,notification_type,title,body) SELECT ?,room_id,id,?,?,? FROM room_memberships WHERE room_id=? AND id<>?",UUID.randomUUID().toString(),type,title,body,roomId,actor);}
    private void audit(String room,String expense,String actor,String action,String description){String safe=description.length()>1200?description.substring(0,1197)+"…":description;db.update("INSERT INTO room_expense_audit(id,room_id,expense_id,actor_membership_id,actor_display_name,action,description) VALUES (?,?,?,?,?,?,?)",UUID.randomUUID().toString(),room,expense,actor,memberName(room,actor),action,safe);}
    private String expenseSnapshot(String roomId,String expenseId){return db.query("SELECT e.title||' · '||e.total_amount||' ₫ · trả: '||COALESCE((SELECT string_agg(p.display_name||' '||p.amount::text,'; ') FROM room_expense_payers p WHERE p.expense_id=e.id),'chưa nhập')||' · chia: '||COALESCE((SELECT string_agg(s.display_name||' '||s.amount::text,'; ') FROM room_expense_shares s WHERE s.expense_id=e.id),'chưa nhập') FROM room_expenses e WHERE e.id=? AND e.room_id=?",rs->rs.next()?rs.getString(1):null,expenseId,roomId);}
    private static String planSnapshot(Room room){String value=room.getName()+" · "+Objects.toString(room.getEventDate(),"chưa chọn ngày")+" "+Objects.toString(room.getEventTime(),"")+" · "+Objects.toString(room.getRegion(),"chưa chọn địa điểm")+" · "+Objects.toString(room.getPlanNotes(),"");return value.length()>900?value.substring(0,897)+"…":value;}
    private void afterCommit(Runnable callback){if(TransactionSynchronizationManager.isSynchronizationActive())TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization(){@Override public void afterCommit(){callback.run();}});else callback.run();}
    private String memberName(String roomId,String memberId){return db.query("SELECT display_name FROM room_memberships WHERE room_id=? AND id=?",rs->rs.next()?rs.getString(1):"Thành viên đã rời phòng",roomId,memberId);}
    private String participantName(String roomId,String memberId){String name=db.query("SELECT display_name FROM room_memberships WHERE room_id=? AND id=?",rs->rs.next()?rs.getString(1):null,roomId,memberId);if(name!=null)return name;return db.query("SELECT display_name FROM room_expense_payers p JOIN room_expenses e ON e.id=p.expense_id WHERE e.room_id=? AND p.membership_id=? UNION SELECT display_name FROM room_expense_shares s JOIN room_expenses e ON e.id=s.expense_id WHERE e.room_id=? AND s.membership_id=? LIMIT 1",rs->rs.next()?rs.getString(1):"Thành viên đã rời phòng",roomId,memberId,roomId,memberId);}
    private static String clean(String value){return value==null||value.isBlank()?null:value.trim();}
    private record Balance(String id,String name,long balance){}
}
