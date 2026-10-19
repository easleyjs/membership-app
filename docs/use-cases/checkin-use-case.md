
# UC-001: Member Event Check-In

**Status:** Draft  
**Primary Actor:** Authorized Staff User  
**Related Entities:** Member, Event, CheckIn, AppUser

## 1. Overview

### 1.1 Purpose
Check-in allows door staff to verify that a customer is authorized to enter the event (whether that's through membership, or ticket.)
as well as the status of their membership.

### 1.2 Scope
This use case covers check-ins by door/admin staff. Other processes such as ticket purchase, member registration, and offline door functionality are covered elsewhere.

## 2. Preconditions

- The event exists, and is active.
- The member can be identified via member QR code or manual search.
- The requesting staff user is authenticated and authorized.

## 3. Main Success Scenario (Happy Path)

1. Staff selects or scans a member.
2. Staff initiates check-in for an event.
3. Client submits a check-in request.
4. Server verifies authentication and authorization.
5. Server validates the member, event, and eligibility.
6. Server checks for an existing check-in.
7. Server persists the new check-in.
8. Server returns a successful response.

## 4. Alternative Flows and Exceptions

### AF-001: Member Not Found
Client receives a message that the Member was not found.

### AF-002: Member Not Eligible
Staff scans member QR code and sees not eligible status (inactive, banned, no ticket or payment, etc.)
Staff is given option to override and create check-in.

### AF-003: Duplicate Check-In
Staff scans member QR code and finds that the Member is already checked in.
Existing check-in is retained.

### AF-004: Unauthorized Staff User
Staff with a disabled, or non-existing account should not be able to submit a check-in.

### AF-005: Event Not Eligible for Check-In
Staff receives feedback message stating check-in unavailable for this event.

### AF-006: Offline Check-In
See reference for offline synchronization design (UC-002.)

## 5. Business Rules

- **BR-001:** Only authorized staff may perform check-ins.
- **BR-002:** A member must exist before a check-in can be recorded.
- **BR-003:** The event must exist.
- **BR-004:** A member must meet the event's admission requirements.
- **BR-005:** A member may have at most one accepted check-in per event.
- **BR-006:** Repeated synchronization of the same request must not create duplicate check-ins.

*Rules are provisional until confirmed.*

## 6. Inputs and Outputs

### Inputs
- Member identifier - UUID ID# via QR or manual lookup.
- Event identifier - Event ID # via client
- Client-generated request identifier - UUID ID# from Check-in provided by client.
- Client-recorded check-in timestamp (where applicable)
- Authenticated staff identity (from security context)

### Successful Output
- Check-in identifier
- Check-in status
- Relevant timestamps
- Confirmation message, if needed

### Error Outputs
Define the error categories and expected client behavior for validation, authorization, eligibility, and synchronization failures.

#### Validation
- Member not found
- Event not active
- Member already checked in

#### Authorization
- Staff member not authorized to perform check-in

#### Eligibility
- Membership not active
- Member is banned

#### Synchronization
- Check-in could not be saved to db

## 7. Data and Persistence Requirements

- Associate each check-in with a member, event, and staff user.
- Record appropriate creation and occurrence timestamps.
- Prevent duplicate accepted check-ins.
- Support idempotent request processing.
- Ensure database consistency under concurrent requests.

## 8. Acceptance Criteria

### AC-001: Successful Check-In
**Given** an authorized staff user and eligible member  
**When** a valid check-in is submitted  
**Then** the system persists the check-in and returns confirmation.

### AC-002: Duplicate Check-In
**Given** an existing accepted check-in  
**When** another check-in is submitted for the same member and event  
**Then** the system does not create a second accepted check-in.

### AC-003: Unauthorized Check-In
**Given** a user without check-in permission  
**When** a check-in is attempted  
**Then** the request is rejected without modifying check-in records.

### AC-004: Synchronization Retry
**Given** a previously processed check-in request  
**When** the same request is submitted again  
**Then** the server returns the previously recorded outcome without creating a duplicate.

## 9. Open Questions

- Can a nonmember be checked in?
- Does there need to be a way to handle cash cover charge payment
- Can staff override an eligibility failure?
- How should expired memberships be handled?
- Is an offline check-in immediately accepted or provisional?
- What happens when two offline devices check in the same member?
- Which event states permit check-in?

## 10. Related Documentation

- Offline synchronization design (TBD)
- Event admission business rules (TBD)
- CheckIn entity design (TBD)
