package com.architecture.first.framework.security.events;

import com.architecture.first.framework.technical.events.ArchitectureFirstEvent;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@AllArgsConstructor
@Data
public class SecurityHolderEvent {
    private ArchitectureFirstEvent architectureFirstEvent;
}
