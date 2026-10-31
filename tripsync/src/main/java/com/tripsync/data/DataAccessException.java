package com.tripsync.data;

/**
 * 저장소 작업이 실패했을 때 던지는 예외.
 *
 * <p>{@link java.sql.SQLException} 은 검사 예외라서 컨트롤러마다 try-catch 가 번져 나간다.
 * 저장소 계층에서 이 비검사 예외로 한 번 감싸면, 화면 코드는 "저장에 실패하면
 * 사용자에게 안내한다"는 한 가지 처리만 신경 쓰면 된다.</p>
 *
 * <p>설계서 10장의 "저장 실패 → 입력값을 유지하고 재시도 안내" 가 이 예외를 받아 동작한다.</p>
 */
public class DataAccessException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public DataAccessException(String message, Throwable cause) {
        super(message, cause);
    }

    public DataAccessException(String message) {
        super(message);
    }
}
