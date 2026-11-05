package com.letsplay.room;

import jakarta.validation.constraints.NotNull;

public record ProposalCompletionRequest(@NotNull Boolean completed) {}
