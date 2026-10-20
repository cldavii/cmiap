package ai.incorptech.cmiap.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChatMemoryRecordRepository extends JpaRepository<ChatMemoryRecord, String> {

    @Modifying
    @Query(value = """
            INSERT INTO chat_memories (memory_id, messages)
            VALUES (:memoryId, :messages)
            ON CONFLICT (memory_id) DO UPDATE SET messages = EXCLUDED.messages
            """, nativeQuery = true)
    void upsert(@Param("memoryId") String memoryId, @Param("messages") String messages);
}
