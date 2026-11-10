package com.letsplay.room;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "rooms")
public class Room {
    public static final String DEFAULT_AVAILABILITY_TITLE="Giờ nào mọi người đều rảnh?";
    @Id
    @Column(length = 36)
    private String id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "invite_code", nullable = false, unique = true, length = 8)
    private String inviteCode;

    @Column(name = "creation_request_id", nullable = false, unique = true, length = 36)
    private String creationRequestId;

    @Column(nullable = false, length = 20)
    private String phase;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "event_date")
    private LocalDate eventDate;

    @Column(name = "event_time")
    private LocalTime eventTime;

    @Column(length = 120)
    private String region;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private long revision;

    @Column(name="selected_proposal_id",length=36)
    private String selectedProposalId;

    @Column(name="selected_proposal_ids",columnDefinition="TEXT")
    private String selectedProposalIds;

    @Column(name="ended_at")
    private Instant endedAt;

    @Column(name="updated_at",nullable=false)
    private Instant updatedAt;

    @Column(name="availability_title",nullable=false,length=100)
    private String availabilityTitle=DEFAULT_AVAILABILITY_TITLE;

    @Column(name="plan_notes",length=2000) private String planNotes;
    @Column(name="event_latitude") private Double eventLatitude;
    @Column(name="event_longitude") private Double eventLongitude;
    @Column(name="meetup_latitude") private Double meetupLatitude;
    @Column(name="meetup_longitude") private Double meetupLongitude;
    @Column(name="weather_fallback",length=1000) private String weatherFallback;

    protected Room() {}

    public Room(String id, String name, String inviteCode, String creationRequestId,
                LocalDate eventDate, LocalTime eventTime, String region) {
        this.id = id;
        this.name = name;
        this.inviteCode = inviteCode;
        this.creationRequestId = creationRequestId;
        this.phase = "COLLECTING";
        this.status = "OPEN";
        this.eventDate = eventDate;
        this.eventTime = eventTime;
        this.region = region;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
        this.revision = 0;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getInviteCode() { return inviteCode; }
    public String getCreationRequestId() { return creationRequestId; }
    public String getPhase() { return phase; }
    public String getStatus() { return status; }
    public LocalDate getEventDate() { return eventDate; }
    public LocalTime getEventTime() { return eventTime; }
    public String getRegion() { return region; }
    public Instant getCreatedAt() { return createdAt; }
    public long getRevision() { return revision; }
    public String getSelectedProposalId() { return selectedProposalId; }
    public String getSelectedProposalIds() { return selectedProposalIds; }
    public void setSelectedProposalIds(String selectedProposalIds) { this.selectedProposalIds = selectedProposalIds; }
    public Instant getEndedAt() { return endedAt; }
    public void setEndedAt(Instant endedAt) { this.endedAt = endedAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public String getAvailabilityTitle() { return availabilityTitle; }
    public String getPlanNotes() { return planNotes; }
    public Double getEventLatitude() { return eventLatitude; }
    public Double getEventLongitude() { return eventLongitude; }
    public Double getMeetupLatitude() { return meetupLatitude; }
    public Double getMeetupLongitude() { return meetupLongitude; }
    public String getWeatherFallback() { return weatherFallback; }
    public void bumpRevision() { this.revision++; this.updatedAt=Instant.now(); }
    public void changePhase(String phase,String selectedProposalId) { this.phase=phase; this.selectedProposalId=selectedProposalId; }
    public void setEventDateTime(java.time.LocalDateTime value) { this.eventDate=value.toLocalDate(); this.eventTime=value.toLocalTime(); }
    public void setAvailabilityTitle(String title) { this.availabilityTitle=title; }
    public void updatePlan(String name,LocalDate date,LocalTime time,String region,String notes,Double latitude,Double longitude,String fallback) {
        this.name=name; this.eventDate=date; this.eventTime=time; this.region=region; this.planNotes=notes;
        this.eventLatitude=latitude; this.eventLongitude=longitude; this.weatherFallback=fallback;
    }
    public void setMeetupPoint(Double latitude,Double longitude) { this.meetupLatitude=latitude; this.meetupLongitude=longitude; }
    public void setStatus(String status) { this.status=status; }
}
