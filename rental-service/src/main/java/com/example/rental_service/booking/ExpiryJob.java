package com.example.rental_service.booking;
@org.springframework.stereotype.Component
public class ExpiryJob {
    private final BookingService service;
    public ExpiryJob(BookingService service){this.service=service;}
    @org.springframework.scheduling.annotation.Scheduled(fixedDelay=60000,initialDelay=60000)
    public void expire(){for(long id:service.due())try{service.expire(id);}catch(RuntimeException e){org.slf4j.LoggerFactory.getLogger(ExpiryJob.class).warn("Could not expire booking {}",id);}}
}
