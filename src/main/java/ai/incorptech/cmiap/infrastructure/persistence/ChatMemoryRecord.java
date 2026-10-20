package ai.incorptech.cmiap.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "chat_memories")
@NoArgsConstructor
@Getter
@Setter
public class ChatMemoryRecord {

    @Id
    @Column(name = "memory_id", nullable = false, length = 512)
    private String memoryId;

    @Column(name = "messages", nullable = false, columnDefinition = "text")
    private String messages;

    public ChatMemoryRecord(String memoryId, String messages) {
        this.memoryId = memoryId;
        this.messages = messages;
    }
}
