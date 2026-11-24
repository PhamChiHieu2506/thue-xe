package com.example.rental_service.booking;
import com.example.rental_service.client.CarClient;
import com.example.rental_service.dto.CarResponse;
import java.util.*;
import java.time.*;
import java.math.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
@Service
public class BookingService {
    private static final org.slf4j.Logger log=org.slf4j.LoggerFactory.getLogger(BookingService.class);
    private final BookingRepository bookings;private final PaymentRepository payments;private final EventRepository events;
    private final BillingRepository billing;private final JdbcTemplate jdbc;private final CarClient cars;
    private final int depositPercent,holdMinutes;
    public BookingService(BookingRepository bookings,PaymentRepository payments,EventRepository events,BillingRepository billing,JdbcTemplate jdbc,CarClient cars,
        @Value("${app.booking.deposit-percent:40}") int depositPercent,@Value("${app.booking.payment-hold-minutes:1440}") int holdMinutes){
        this.bookings=bookings;this.payments=payments;this.events=events;this.billing=billing;this.jdbc=jdbc;this.cars=cars;
        if(depositPercent<1||depositPercent>100||holdMinutes<1)throw new IllegalArgumentException("Invalid booking policy");this.depositPercent=depositPercent;this.holdMinutes=holdMinutes;
    }
    private String subject(){return SecurityContextHolder.getContext().getAuthentication().getName();}
    private boolean role(String r){return SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream().anyMatch(a->a.getAuthority().equals("ROLE_"+r));}
    private void forbid(boolean condition){if(!condition)throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Không có quyền với chuyến xe này");}
    private CarResponse car(long id){
        var token=(JwtAuthenticationToken)SecurityContextHolder.getContext().getAuthentication();
        CarResponse c=cars.getCarById(id,"Bearer "+token.getToken().getTokenValue()).getBody();
        if(c==null||c.getId()==null||c.getId()!=id||c.getOwnerId()==null||!Boolean.TRUE.equals(c.getPublished())||c.getPricePerDay()==null||c.getPricePerDay().signum()<=0)throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,"Dịch vụ xe trả dữ liệu không hợp lệ hoặc xe chưa được đăng");
        if(!"AVAILABLE".equals(c.getStatus()))throw new ResponseStatusException(HttpStatus.CONFLICT,"Xe hiện chưa sẵn sàng cho thuê");return c;
    }
    private int days(Instant start,Instant end){if(start==null||end==null||start.isBefore(Instant.now().minusSeconds(60))||!end.isAfter(start))throw new IllegalArgumentException("Chọn thời gian nhận/trả hợp lệ trong tương lai");long millis=Duration.between(start,end).toMillis();if(millis>Duration.ofDays(365).toMillis())throw new IllegalArgumentException("Mỗi chuyến tối đa 365 ngày");return Math.toIntExact((millis+86399999)/86400000);}
    private boolean occupied(long carId,Instant start,Instant end,Long excluded){return bookings.blocking(start,end,Instant.now()).stream().anyMatch(b->b.carId==carId&&!Objects.equals(b.id,excluded));}
    @Transactional(readOnly=true) public List<Long> unavailable(Instant start,Instant end){days(start,end);return bookings.blocking(start,end,Instant.now()).stream().map(b->b.carId).distinct().toList();}
    public record Quote(long carId,String carName,String imageUrl,String ownerId,int days,Instant startsAt,Instant endsAt,BigDecimal pricePerDay,BigDecimal totalPrice,BigDecimal deposit,int depositPercent,String currency){}
    @Transactional(readOnly=true) public Quote quote(long carId,Instant start,Instant end){int days=days(start,end);CarResponse c=car(carId);if(occupied(carId,start,end,null))throw new ResponseStatusException(HttpStatus.CONFLICT,"Xe đã có lịch thuê trong khoảng thời gian này");BigDecimal total=c.getPricePerDay().multiply(BigDecimal.valueOf(days)).setScale(2);return new Quote(carId,c.getName(),c.getImageUrl(),c.getOwnerId(),days,start,end,c.getPricePerDay(),total,total.multiply(BigDecimal.valueOf(depositPercent)).divide(BigDecimal.valueOf(100),2,RoundingMode.HALF_UP),depositPercent,"VND");}
    private void calendar(long carId){jdbc.update("INSERT INTO booking_calendars(car_id) VALUES (?) ON CONFLICT DO NOTHING",carId);jdbc.queryForObject("SELECT car_id FROM booking_calendars WHERE car_id=? FOR UPDATE",Long.class,carId);}
    private Booking locked(long id){Booking first=bookings.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Không tìm thấy chuyến xe"));calendar(first.carId);return bookings.locked(id).orElseThrow();}
    private void event(Booking b,String action,String note){b.updatedAt=Instant.now();events.save(new BookingEvent(b.id,action,subject(),note));log.info("Booking {} action {}",b.id,action);}
    private void reader(Booking b){forbid(role("ADMIN")||b.customerId.equals(subject())||b.ownerId.equals(subject()));}
    private void owner(Booking b){forbid(role("ADMIN")||b.ownerId.equals(subject()));}
    private boolean expired(Booking b){return "APPROVED".equals(b.status)&&b.paymentDeadline!=null&&!b.paymentDeadline.isAfter(Instant.now());}
    private void noExpired(Booking b){if(expired(b))throw new ResponseStatusException(HttpStatus.CONFLICT,"Đã hết hạn thanh toán giữ chỗ");}
    public record Create(long carId,Instant startsAt,Instant endsAt,BigDecimal expectedTotal,String message){}
    @Transactional public Booking create(Create request,String key){
        forbid(role("CUSTOMER"));if(key==null||!key.matches("[A-Za-z0-9_-]{16,80}"))throw new IllegalArgumentException("Cần Idempotency-Key hợp lệ");
        var previous=bookings.findByCustomerIdAndRequestKey(subject(),key);if(previous.isPresent()){Booking b=previous.get();if(b.carId!=request.carId()||!b.startsAt.equals(request.startsAt())||!b.endsAt.equals(request.endsAt())||request.expectedTotal()==null||b.totalPrice.compareTo(request.expectedTotal())!=0)throw new ResponseStatusException(HttpStatus.CONFLICT,"Mã yêu cầu đã dùng cho thông tin khác");return b;}
        Quote q=quote(request.carId(),request.startsAt(),request.endsAt());
        if(q.ownerId().equals(subject()))throw new IllegalArgumentException("Không thể tự đặt xe của mình");
        if(request.expectedTotal()==null||request.expectedTotal().compareTo(q.totalPrice())!=0)throw new ResponseStatusException(HttpStatus.CONFLICT,"Giá xe đã thay đổi. Hãy xem lại báo giá");
        calendar(request.carId());
        previous=bookings.findByCustomerIdAndRequestKey(subject(),key);if(previous.isPresent()){Booking b=previous.get();if(b.carId!=request.carId()||!b.startsAt.equals(request.startsAt())||!b.endsAt.equals(request.endsAt())||b.totalPrice.compareTo(request.expectedTotal())!=0)throw conflict();return b;}
        if(occupied(request.carId(),request.startsAt(),request.endsAt(),null))throw new ResponseStatusException(HttpStatus.CONFLICT,"Xe vừa có lịch thuê khác");
        Booking b=new Booking();b.carId=q.carId();b.ownerId=q.ownerId();b.customerId=subject();b.requestKey=key;b.carName=q.carName();b.imageUrl=q.imageUrl();b.startsAt=q.startsAt();b.endsAt=q.endsAt();b.days=q.days();b.pricePerDay=q.pricePerDay();b.totalPrice=q.totalPrice();b.deposit=q.deposit();b.message=text(request.message(),1000);bookings.saveAndFlush(b);event(b,"REQUESTED","Khách gửi yêu cầu thuê xe");return b;
    }
    @Transactional(readOnly=true) public List<Booking> list(){return role("ADMIN")?bookings.findAll(org.springframework.data.domain.Sort.by("createdAt").descending()):role("OWNER")?bookings.findByOwnerIdOrderByCreatedAtDesc(subject()):bookings.findByCustomerIdOrderByCreatedAtDesc(subject());}
    @Transactional(readOnly=true) public Booking get(long id){Booking b=bookings.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));reader(b);return b;}
    @Transactional public Booking action(long id,String action,String note){
        Booking b=locked(id);noExpired(b);String reason=text(note,500);
        switch(action){
            case "approve"->{owner(b);if(!"PENDING".equals(b.status)||!b.startsAt.isAfter(Instant.now()))throw conflict();CarResponse c=car(b.carId);if(!c.getOwnerId().equals(b.ownerId))throw conflict();if(occupied(b.carId,b.startsAt,b.endsAt,b.id))throw new ResponseStatusException(HttpStatus.CONFLICT,"Đã có chuyến khác trùng lịch");if(reason==null||reason.isBlank())throw new IllegalArgumentException("Cần địa chỉ và hướng dẫn bàn giao xe");b.handoverAddress=reason;b.status="APPROVED";b.paymentDeadline=Instant.now().plusSeconds(holdMinutes*60L);if(b.paymentDeadline.isAfter(b.startsAt))b.paymentDeadline=b.startsAt;}
            case "reject"->{owner(b);if(!"PENDING".equals(b.status))throw conflict();requiredReason(reason);b.status="REJECTED";b.reason=reason;}
            case "cancel"->{reader(b);if(!Set.of("PENDING","APPROVED","CONFIRMED").contains(b.status))throw conflict();requiredReason(reason);b.status="CANCELLED";b.reason=reason;b.refundDue=b.paid;}
            case "start"->{owner(b);if(!"CONFIRMED".equals(b.status)||!b.endsAt.isAfter(Instant.now())||Instant.now().isBefore(b.startsAt.minusSeconds(7200)))throw new ResponseStatusException(HttpStatus.CONFLICT,"Chỉ bàn giao xe đã xác nhận, trong vòng 2 giờ trước lịch nhận");b.status="IN_PROGRESS";b.actualStart=Instant.now();}
            case "complete"->{owner(b);if(!"IN_PROGRESS".equals(b.status)||b.paid.compareTo(b.totalPrice)<0)throw new ResponseStatusException(HttpStatus.CONFLICT,"Cần nhận xe và thanh toán đủ trước khi hoàn tất");b.status="COMPLETED";b.actualEnd=Instant.now();}
            case "refund"->{owner(b);if(!Set.of("CANCELLED","EXPIRED").contains(b.status)||b.refundDue.signum()<=0||b.refundedAt!=null)throw conflict();requiredReason(reason);b.refundReference=text(reason,100);b.refundedAt=Instant.now();}
            default->throw new IllegalArgumentException("Thao tác không hợp lệ");
        }
        event(b,action.toUpperCase(Locale.ROOT),reason);return bookings.save(b);
    }
    public record Pay(BigDecimal amount,String reference,String note){}
    @Transactional public PaymentClaim claim(long id,Pay request,boolean cash){
        Booking b=locked(id);noExpired(b);if(cash)owner(b);else forbid(b.customerId.equals(subject()));
        if(!Set.of("APPROVED","CONFIRMED","IN_PROGRESS").contains(b.status))throw conflict();
        BigDecimal amount=money(request.amount());String ref=text(request.reference(),100);if(ref==null||ref.length()<4)throw new IllegalArgumentException("Cần mã tham chiếu nhận tiền");
        var old=payments.findByBookingIdAndReference(id,ref);if(old.isPresent()){if(old.get().amount.compareTo(amount)!=0)throw conflict();return old.get();}
        if(amount.compareTo(b.totalPrice.subtract(b.paid))>0)throw new IllegalArgumentException("Số tiền vượt phần còn phải thanh toán");
        PaymentClaim p=new PaymentClaim();p.bookingId=id;p.amount=amount;p.reference=ref;p.note=text(request.note(),500);
        if(cash){p.method="CASH";p.status="VERIFIED";p.confirmedAt=Instant.now();credit(b,amount);}
        payments.saveAndFlush(p);event(b,cash?"CASH_RECEIVED":"PAYMENT_SUBMITTED",cash?"Chủ xe ghi nhận tiền đã thực nhận":"Khách gửi mã tham chiếu chuyển khoản");return p;
    }
    @Transactional public PaymentClaim verify(long bookingId,long paymentId,boolean accepted,String note){
        Booking b=locked(bookingId);owner(b);noExpired(b);PaymentClaim p=payments.findById(paymentId).filter(x->x.bookingId==bookingId).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
        if(!"PENDING".equals(p.status))return p;
        if(!Set.of("APPROVED","CONFIRMED","IN_PROGRESS").contains(b.status))throw conflict();
        if(accepted){if(p.amount.compareTo(b.totalPrice.subtract(b.paid))>0)throw new IllegalArgumentException("Số tiền vượt phần còn phải thanh toán");credit(b,p.amount);p.status="VERIFIED";p.confirmedAt=Instant.now();}else{p.status="REJECTED";p.note=text(note,500);}
        event(b,accepted?"PAYMENT_VERIFIED":"PAYMENT_REJECTED",accepted?"Chủ xe xác nhận đã thực nhận tiền":p.note);return payments.save(p);
    }
    private void credit(Booking b,BigDecimal amount){b.paid=b.paid.add(amount);if("APPROVED".equals(b.status)&&b.paid.compareTo(b.deposit)>=0)b.status="CONFIRMED";bookings.save(b);}
    @Transactional(readOnly=true) public List<PaymentClaim> payments(long id){get(id);return payments.findByBookingIdOrderByCreatedAtAsc(id);}
    @Transactional(readOnly=true) public List<BookingEvent> events(long id){get(id);return events.findByBookingIdOrderByCreatedAtAsc(id);}
    @Transactional public void expire(long id){Booking b=locked(id);if(expired(b)){b.status="EXPIRED";b.reason="Hết hạn thanh toán giữ chỗ";b.refundDue=b.paid;b.updatedAt=Instant.now();events.save(new BookingEvent(b.id,"EXPIRED","SYSTEM",b.reason));bookings.save(b);}}
    @Transactional(readOnly=true) public List<Long> due(){return bookings.expired(Instant.now());}
    @Transactional(readOnly=true) public Map<String,Object> billing(boolean full,String ownerId){
        BillingSettings b=billing.findById(ownerId).orElse(null);if(b==null)return Map.of("method","CASH","configured",false);
        Map<String,Object> result=new LinkedHashMap<>();result.put("method",b.method);result.put("configured",true);result.put("bankName",b.bankName);
        result.put("accountNumber",full?b.accountNumber:mask(b.accountNumber));result.put("accountHolder",full?b.accountHolder:mask(b.accountHolder));return result;
    }
    @Transactional(readOnly=true) public Map<String,Object> instructions(long id){Booking b=get(id);if(!Set.of("APPROVED","CONFIRMED","IN_PROGRESS","COMPLETED").contains(b.status))throw conflict();return billing(true,b.ownerId);}
    public record BillingInput(String method,String bankName,String accountNumber,String accountHolder){}
    @Transactional public Map<String,Object> saveBilling(BillingInput input){forbid(role("OWNER")||role("ADMIN"));BillingSettings b=billing.findById(subject()).orElseGet(BillingSettings::new);b.ownerId=subject();if(!Set.of("CASH","BANK_TRANSFER").contains(String.valueOf(input.method())))throw new IllegalArgumentException("Phương thức không hợp lệ");b.method=input.method();if("BANK_TRANSFER".equals(b.method)){b.bankName=text(input.bankName(),100);b.accountNumber=text(input.accountNumber(),40);b.accountHolder=text(input.accountHolder(),150);if(b.bankName==null||b.accountNumber==null||b.accountHolder==null||!b.accountNumber.matches("[A-Za-z0-9]{4,40}"))throw new IllegalArgumentException("Cần nhập đủ thông tin ngân hàng");}if("CASH".equals(b.method)){b.bankName=null;b.accountNumber=null;b.accountHolder=null;}billing.save(b);return billing(false,subject());}
    public String currentSubject(){return subject();}
    public String visibleStatus(Booking b){return expired(b)?"EXPIRED":b.status;}
    private static String mask(String s){return s==null?null:s.length()<=3?"***":"*".repeat(s.length()-3)+s.substring(s.length()-3);}
    private static String text(String value,int limit){if(value==null)return null;String s=value.trim();if(s.length()>limit||s.chars().anyMatch(c->Character.isISOControl(c)&&c!='\n'))throw new IllegalArgumentException("Nội dung không hợp lệ hoặc quá dài");return s;}
    private static void requiredReason(String s){if(s==null||s.length()<4)throw new IllegalArgumentException("Cần nêu lý do hoặc mã tham chiếu đầy đủ");}
    private static BigDecimal money(BigDecimal amount){if(amount==null||amount.signum()<=0)throw new IllegalArgumentException("Số tiền cần lớn hơn 0");try{amount=amount.setScale(2,RoundingMode.UNNECESSARY);}catch(ArithmeticException ex){throw new IllegalArgumentException("Số tiền tối đa 2 số thập phân");}if(amount.precision()>18)throw new IllegalArgumentException("Số tiền vượt giới hạn");return amount;}
    private static ResponseStatusException conflict(){return new ResponseStatusException(HttpStatus.CONFLICT,"Trạng thái chuyến xe không cho phép thao tác này");}
}
