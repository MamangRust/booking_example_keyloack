package com.sanedge.booking_keyclock.service.impl;

import java.io.File;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.sanedge.booking_keyclock.domain.request.room.CreateRoomRequest;
import com.sanedge.booking_keyclock.domain.request.room.UpdateRoomRequest;
import com.sanedge.booking_keyclock.domain.response.MessageResponse;
import com.sanedge.booking_keyclock.domain.response.room.RoomResponse;
import com.sanedge.booking_keyclock.enums.RoomStatus;
import com.sanedge.booking_keyclock.mapper.RoomMapper;
import com.sanedge.booking_keyclock.models.Room;
import com.sanedge.booking_keyclock.repository.RoomRepository;
import com.sanedge.booking_keyclock.service.RoomService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class RoomServiceImpl implements RoomService {
    private final RoomRepository roomRepository;
    private final FileServiceImpl fileService;
    private final FolderServiceImpl folderService;

    @Autowired
    public RoomServiceImpl(RoomRepository roomRepository, FileServiceImpl fileService,
            FolderServiceImpl folderService) {
        this.roomRepository = roomRepository;
        this.fileService = fileService;
        this.folderService = folderService;
    }

    public MessageResponse findAll() {
        try {
            log.info("Fetching all rooms");

            List<Room> roomList = roomRepository.findAll();
            return MessageResponse.builder()
                    .message("Success")
                    .data(RoomMapper.toRoomResponseList(roomList))
                    .statusCode(HttpStatus.OK.value())
                    .build();
        } catch (Exception e) {
            log.error("Error occurred while fetching rooms", e);
            return MessageResponse.builder()
                    .message("Error occurred while fetching rooms")
                    .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .build();
        }
    }

    public MessageResponse findById(Long id) {
        try {
            log.info("Fetching room by ID: {}", id);

            Optional<Room> room = roomRepository.findById(id);
            if (room.isPresent()) {
                log.info("Room found: {}", room.get());
                return MessageResponse.builder()
                        .message("Success")
                        .data(RoomMapper.toRoomResponse(room.get()))
                        .statusCode(HttpStatus.OK.value())
                        .build();
            } else {
                log.info("Room not found");
                return MessageResponse.builder()
                        .message("Room not found")
                        .statusCode(HttpStatus.NOT_FOUND.value())
                        .build();
            }
        } catch (Exception e) {
            log.error("Error occurred while fetching room by ID", e);
            return MessageResponse.builder()
                    .message("Error occurred while fetching room by ID")
                    .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .build();
        }
    }

    @Override
    public MessageResponse createRoom(CreateRoomRequest createRoomRequest) {
        log.info("Creating room: {}", createRoomRequest);

        MultipartFile myFile = createRoomRequest.getFile();

        Room room = new Room();
        room.setRoomName(createRoomRequest.getRoomName());
        room.setRoomCapacity(createRoomRequest.getRoomCapacity());
        room.setRoomStatus(RoomStatus.READY);

        Optional<Room> findName = roomRepository.findByRoomName(createRoomRequest.getRoomName());
        if (findName.isPresent()) {
            return MessageResponse.builder()
                    .message("Room name already exists")
                    .statusCode(HttpStatus.BAD_REQUEST.value())
                    .build();
        }

        try {
            if (myFile != null) {
                String folderPath = folderService.createFolder(createRoomRequest.getRoomName());

                if (folderPath != null) {
                    String filePath = folderPath + File.separator + myFile.getOriginalFilename();

                    String createdFilePath = fileService.createFileImage(myFile, filePath);

                    if (createdFilePath != null) {
                        room.setPhoto(createdFilePath);
                        roomRepository.save(room);

                        RoomResponse mapper = RoomMapper.toRoomResponse(room);

                        log.info("Room created successfully: {}", mapper);

                        return MessageResponse.builder()
                                .message("Room created successfully")
                                .data(mapper)
                                .statusCode(HttpStatus.OK.value())
                                .build();
                    } else {
                        log.error("Failed to create the file");
                        return MessageResponse.builder()
                                .message("Failed to create the file")
                                .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                                .build();
                    }
                } else {
                    log.error("Failed to create the folder");
                    return MessageResponse.builder()
                            .message("Failed to create the folder")
                            .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                            .build();
                }
            }
        } catch (Exception e) {
            log.error("Unexpected error occurred", e);
            return MessageResponse.builder()
                    .message("Unexpected error occurred")
                    .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .build();
        }

        log.error("No file provided");

        return MessageResponse.builder()
                .message("No file provided")
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .build();
    }

    public MessageResponse updateRoom(Long id, UpdateRoomRequest request) {
        log.info("Updating room with id: {}", id);

        Optional<Room> findName = roomRepository.findByRoomName(request.getRoomName());
        if (findName.isPresent()) {
            return MessageResponse.builder()
                    .message("Room name already exists")
                    .statusCode(HttpStatus.BAD_REQUEST.value())
                    .build();
        }

        MultipartFile myFile = request.getFile();

        Room room = roomRepository.findById(id).orElseThrow(() -> new RuntimeException("Room not found"));

        room.setRoomName(request.getRoomName());
        room.setRoomCapacity(request.getRoomCapacity());

        try {
            log.info("Updating room with id: {}", id);
            if (myFile != null) {
                folderService.deleteFolder(room.getRoomName());
                fileService.deleteFileImage(room.getPhoto());

                String folderPath = folderService.createFolder(request.getRoomName());

                if (folderPath != null) {
                    log.info("Folder created successfully: {}", folderPath);

                    String filePath = folderPath + File.separator + myFile.getOriginalFilename();
                    String createdFilePath = fileService.createFileImage(myFile, filePath);

                    if (createdFilePath != null) {
                        room.setPhoto(createdFilePath);
                        roomRepository.save(room);

                        RoomResponse mapper = RoomMapper.toRoomResponse(room);

                        log.info("Room updated successfully: {}", mapper);

                        return MessageResponse.builder().message("Room updated successfully").data(mapper)
                                .statusCode(HttpStatus.OK.value()).build();
                    } else {
                        log.error("Failed to create the file");
                        return MessageResponse.builder()
                                .message("Failed to create the file")
                                .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                                .build();
                    }
                } else {
                    log.error("Failed to create the folder");
                    return MessageResponse.builder()
                            .message("Failed to create the folder")
                            .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                            .build();
                }

            }

        } catch (Exception e) {
            log.error("Unexpected error occurred", e);

            return MessageResponse.builder()
                    .message("Unexpected error occurred")
                    .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .build();
        }

        log.error("No file provided");
        return MessageResponse.builder()
                .message("No file provided")
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .build();

    }

    public MessageResponse deleteRoom(Long id) {
        log.info("Deleting room with id: {}", id);

        Room room = this.roomRepository.findById(id).orElseThrow(() -> new RuntimeException("Room not found"));

        try {
            log.info("Deleting room with id: {}", id);

            roomRepository.delete(room);

            folderService.deleteFolder(room.getRoomName());
            fileService.deleteFileImage(room.getPhoto());

            return MessageResponse.builder()
                    .message("Success")
                    .statusCode(HttpStatus.OK.value())
                    .build();
        } catch (Exception e) {
            log.error("Error occurred while deleting room", e);

            return MessageResponse.builder()
                    .message("Error occurred while deleting room")
                    .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .build();
        }
    }

}
