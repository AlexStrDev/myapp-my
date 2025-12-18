package com.example.pixelplace.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class CanvasCreatedEvent {

    private String canvasId;
    private String name;
    private Integer width;
    private Integer height;
    private String backgroundColor;
    private String createdBy;
}