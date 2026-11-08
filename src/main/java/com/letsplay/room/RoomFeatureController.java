package com.letsplay.room;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/rooms/{roomId}")
@Tag(name="Planning and expenses · Kế hoạch & chi tiêu",description="Cập nhật kế hoạch, xác nhận tham gia, phối hợp đi lại và chia sẻ chi phí trong phòng.")
public class RoomFeatureController {
    private final RoomFeatureService service;
    public RoomFeatureController(RoomFeatureService service){this.service=service;}
    @PutMapping("/plan") @Operation(summary="Cập nhật hoặc hủy kế hoạch")
    public RoomResponse plan(@PathVariable String roomId,@RequestHeader(value="Authorization",required=false) String auth,@Valid @RequestBody RoomFeatureRequests.Plan request){return service.updatePlan(roomId,bearer(auth),request);}
    @PutMapping("/attendance") @Operation(summary="Xác nhận tình trạng tham gia")
    public RoomResponse attendance(@PathVariable String roomId,@RequestHeader(value="Authorization",required=false) String auth,@Valid @RequestBody RoomFeatureRequests.Attendance request){return service.setAttendance(roomId,bearer(auth),request.status());}
    @PutMapping("/meetup-point") @ResponseStatus(HttpStatus.NO_CONTENT) @Operation(summary="Đặt điểm xuất phát chung để tính khoảng cách")
    public void meetupPoint(@PathVariable String roomId,@RequestHeader(value="Authorization",required=false) String auth,@RequestBody Map<String,Double> point){service.setMeetupPoint(roomId,bearer(auth),point.get("latitude"),point.get("longitude"));}
    @PutMapping("/preferences") @ResponseStatus(HttpStatus.NO_CONTENT) @Operation(summary="Lưu ngân sách và sở thích của tôi trong phòng")
    public void preferences(@PathVariable String roomId,@RequestHeader(value="Authorization",required=false) String auth,@Valid @RequestBody RoomFeatureRequests.Preferences request){service.savePreferences(roomId,bearer(auth),request);}
    @PutMapping("/ride") @ResponseStatus(HttpStatus.NO_CONTENT) @Operation(summary="Cập nhật xe và khu vực đón")
    public void ride(@PathVariable String roomId,@RequestHeader(value="Authorization",required=false) String auth,@Valid @RequestBody RoomFeatureRequests.Ride request){service.saveRide(roomId,bearer(auth),request);}
    @GetMapping("/collaboration") @Operation(summary="Xem trạng thái tham gia, sở thích, xe và thông báo")
    public Map<String,Object> collaboration(@PathVariable String roomId,@RequestHeader(value="Authorization",required=false) String auth){return service.collaboration(roomId,bearer(auth));}
    @PostMapping("/notifications/read") @ResponseStatus(HttpStatus.NO_CONTENT) @Operation(summary="Đánh dấu thông báo đã đọc")
    public void readNotifications(@PathVariable String roomId,@RequestHeader(value="Authorization",required=false) String auth){service.markNotificationsRead(roomId,bearer(auth));}
    @GetMapping("/expenses") @Operation(summary="Xem sổ chi tiêu, số dư và lịch sử chỉnh sửa")
    public Map<String,Object> expenses(@PathVariable String roomId,@RequestHeader(value="Authorization",required=false) String auth){return service.expenses(roomId,bearer(auth));}
    @PostMapping("/expenses") @Operation(summary="Thêm khoản chi có nhiều người thanh toán")
    public Map<String,Object> createExpense(@PathVariable String roomId,@RequestHeader(value="Authorization",required=false) String auth,@Valid @RequestBody RoomFeatureRequests.Expense request){return service.saveExpense(roomId,null,bearer(auth),request);}
    @PutMapping("/expenses/{expenseId}") @Operation(summary="Sửa khoản chi và phần chia")
    public Map<String,Object> updateExpense(@PathVariable String roomId,@PathVariable String expenseId,@RequestHeader(value="Authorization",required=false) String auth,@Valid @RequestBody RoomFeatureRequests.Expense request){return service.saveExpense(roomId,expenseId,bearer(auth),request);}
    @DeleteMapping("/expenses/{expenseId}") @ResponseStatus(HttpStatus.NO_CONTENT) @Operation(summary="Xóa khoản chi; lịch sử xóa vẫn được lưu")
    public void deleteExpense(@PathVariable String roomId,@PathVariable String expenseId,@RequestHeader(value="Authorization",required=false) String auth){service.deleteExpense(roomId,expenseId,bearer(auth));}
    @PutMapping("/settlements/{settlementId}") @Operation(summary="Đánh dấu khoản chuyển tiền đã thanh toán hoặc đang chờ")
    public Map<String,Object> settlement(@PathVariable String roomId,@PathVariable String settlementId,@RequestHeader(value="Authorization",required=false) String auth,@Valid @RequestBody RoomFeatureRequests.SettlementStatus request){return service.setSettlement(roomId,settlementId,bearer(auth),request.status());}
    @PostMapping("/settlements") @Operation(summary="Ghi nhận khoản chuyển tiền giữa hai thành viên")
    public Map<String,Object> createSettlement(@PathVariable String roomId,@RequestHeader(value="Authorization",required=false) String auth,@Valid @RequestBody RoomFeatureRequests.Settlement request){return service.createSettlement(roomId,bearer(auth),request);}
    @GetMapping("/calendar-link") @Operation(summary="Đọc liên kết sự kiện Google Calendar của tôi")
    public Map<String,Object> calendarLink(@PathVariable String roomId,@RequestHeader(value="Authorization",required=false) String auth){return service.calendarLink(roomId,bearer(auth));}
    @PutMapping("/calendar-link") @ResponseStatus(HttpStatus.NO_CONTENT) @Operation(summary="Lưu liên kết đồng bộ Google Calendar")
    public void saveCalendarLink(@PathVariable String roomId,@RequestHeader(value="Authorization",required=false) String auth,@Valid @RequestBody RoomFeatureRequests.CalendarLink request){service.saveCalendarLink(roomId,bearer(auth),request.eventId());}
    private static String bearer(String authorization){return authorization!=null&&authorization.startsWith("Bearer ")?authorization.substring(7):null;}
}
