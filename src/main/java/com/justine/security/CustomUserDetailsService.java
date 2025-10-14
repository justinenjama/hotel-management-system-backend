package com.justine.security;

import com.justine.model.Guest;
import com.justine.model.Staff;
import com.justine.repository.GuestRepository;
import com.justine.repository.StaffRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final GuestRepository guestRepository;
    private final StaffRepository staffRepository;

    /**
     * Load user by email (not used by cookies, but required by interface)
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // Check staff first
        Staff staff = staffRepository.findByEmail(username).orElse(null);
        if (staff != null) {
            return mapStaffToUserDetails(staff);
        }

        // Check guest
        Guest guest = guestRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        return mapGuestToUserDetails(guest);
    }

    /**
     * Load user by ID for JWT cookie validation
     */
    public UserDetails loadUserById(Long id, boolean isStaff) {
        if (isStaff) {
            return staffRepository.findById(id)
                    .map(this::mapStaffToUserDetails)
                    .orElseThrow(() -> new UsernameNotFoundException("Staff not found with id: " + id));
        } else {
            return guestRepository.findById(id)
                    .map(this::mapGuestToUserDetails)
                    .orElseThrow(() -> new UsernameNotFoundException("Guest not found with id: " + id));
        }
    }

    private UserDetails mapStaffToUserDetails(Staff staff) {

        return User.builder()
                .username(staff.getId().toString())
                .password(staff.getPassword())
                .authorities("ROLE_" + staff.getRole().name())
                .build();
    }

    private UserDetails mapGuestToUserDetails(Guest guest) {
        return User.builder()
                .username(guest.getId().toString())
                .password(guest.getPassword())
                .authorities("ROLE_" + guest.getRole())
                .build();
    }
}
