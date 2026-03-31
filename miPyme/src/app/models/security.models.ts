export interface AccessLog {
    id: number;
    username: string;
    timestamp: string;
    ipAddress: string;
    device: string;
    location: string;
    medium: string;
}

export interface ActiveSession {
    id: number;
    username: string;
    token: string;
    created: string;
    lastActive: string;
    ipAddress: string;
    device: string;
    location: string;
    medium: string;
    isCurrent?: boolean;
}
