package com.example.pixelplace.command;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class CreateCanvasCommand {

    @TargetAggregateIdentifier
    private String canvasId;

    private String name;
    private Integer width;
    private Integer height;
    private String backgroundColor;
    private String createdBy;
}