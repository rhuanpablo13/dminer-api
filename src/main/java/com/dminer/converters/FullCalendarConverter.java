package com.dminer.converters;

import java.util.Optional;

import com.dminer.dto.FullCalendarDTO;
import com.dminer.dto.FullCalendarRequestDTO;
import com.dminer.entities.FullCalendar;
import com.dminer.entities.User;
import com.dminer.services.UserService;
import com.dminer.utils.UtilDataHora;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class FullCalendarConverter {

    @Autowired
    private UserService userService;


    public FullCalendar dtoToEntity(FullCalendarDTO dto) {
        FullCalendar e = new FullCalendar();
        e.setId(dto.getId());
        e.setAllDay(dto.getAllDay());
        if (dto.getColor() != null && !dto.getColor().isBlank()) {
            e.setColor(dto.getColor());
        }
        if (!dto.getEnd().contains("1970") || !dto.getEnd().contains("1969"))
            e.setEnd(UtilDataHora.toTimestamp(dto.getEnd()));
        
        e.setStart(UtilDataHora.toTimestamp(dto.getStart()));
        e.setTitle(dto.getTitle());
        e.setCreator(dto.getCreator());
        if (! dto.getUsers().isEmpty()) {
            dto.getUsers().forEach(u -> {
                Optional<User>  user = userService.findByLogin(u);
                if (user.isPresent()) {
                    e.getUsers().add(user.get());
                }
            });
        }
        return e;
    }

    public FullCalendar requestDtoToEntity(FullCalendarRequestDTO dto) {
        FullCalendar e = new FullCalendar();        
        e.setAllDay(dto.getAllDay());
        if (dto.getColor() != null && !dto.getColor().isBlank()) {
            e.setColor(dto.getColor());
        }
        if (!dto.getEnd().contains("1970") || !dto.getEnd().contains("1969"))
            e.setEnd(UtilDataHora.toTimestamp(dto.getEnd()));

        e.setStart(UtilDataHora.toTimestamp(dto.getStart()));
        e.setTitle(dto.getTitle());
        e.setCreator(dto.getCreator());
        if (! dto.getUsers().isEmpty()) {
            dto.getUsers().forEach(u -> {
                Optional<User>  user = userService.findByLogin(u);
                if (user.isPresent()) {
                    e.getUsers().add(user.get());
                }
            });
        }
        return e;
    }

    public FullCalendarDTO entityToDto(FullCalendar e) {
        FullCalendarDTO dto = new FullCalendarDTO();
        dto.setId(e.getId());
        dto.setAllDay(e.getAllDay());
        if (e.getColor() != null && !e.getColor().isBlank()) {
            dto.setColor(e.getColor());
        }
        if (e.getEnd() != null) {
            dto.setEnd(UtilDataHora.timestampToString(e.getEnd()));
            if (dto.getEnd().contains("1970") || dto.getEnd().contains("1969")) dto.setEnd(null);
        }
        
        if (e.getStart() != null)
            dto.setStart(UtilDataHora.timestampToString(e.getStart()));
        dto.setTitle(e.getTitle());
        dto.setCreator(e.getCreator());
        if (! e.getUsers().isEmpty()) {
            e.getUsers().forEach(u -> {
                dto.getUsers().add(u.getLogin());
            });
        }

        return dto;
    }

}
