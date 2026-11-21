package com.itsm.chatbot;

/** Pattern: State. Each transition is persisted on the session. */
public enum ChatState { IDLE, AWAIT_SERVICE, AWAIT_DESCRIPTION, AWAIT_IMPACT, AWAIT_CONFIRM, AWAIT_HANDOFF, AWAIT_NOTE_CONFIRM }
