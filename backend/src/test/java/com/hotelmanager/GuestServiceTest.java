package com.hotelmanager;

import com.hotelmanager.repository.GuestRepository;
import com.hotelmanager.service.GuestService;
import com.hotelmanager.web.dto.GuestCreateRequest;
import com.hotelmanager.web.dto.GuestDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class GuestServiceTest {

    @Autowired
    private GuestService guestService;
    @Autowired
    private GuestRepository guestRepository;

    private GuestCreateRequest req(String doc, String email) {
        GuestCreateRequest r = new GuestCreateRequest();
        r.setFirstName("Maria");
        r.setLastName("Lopez");
        r.setEmail(email);
        r.setPhone("+34000000000");
        r.setDocumentNumber(doc);
        r.setNationality("Espana");
        return r;
    }

    @Test
    void createAndRetrieveGuest() {
        GuestDto created = guestService.create(req("DOC-G-1", "maria@example.com"));
        assertNotNull(created.getId());
        assertEquals("Maria", created.getFirstName());
        GuestDto fetched = guestService.get(created.getId());
        assertEquals(created.getId(), fetched.getId());
    }

    @Test
    void searchByNameFindsGuest() {
        guestService.create(req("DOC-G-2", "search@example.com"));
        var page = guestService.search("Maria", PageRequest.of(0, 20));
        assertFalse(page.getContent().isEmpty());
        assertTrue(page.getContent().stream().anyMatch(g -> g.getFirstName().equals("Maria")));
    }

    @Test
    void updateGuestChangesLastName() {
        GuestDto created = guestService.create(req("DOC-G-3", "up@example.com"));
        GuestCreateRequest update = req("DOC-G-3", "up@example.com");
        update.setLastName("Cambio");
        GuestDto updated = guestService.update(created.getId(), update);
        assertEquals("Cambio", updated.getLastName());
    }
}
