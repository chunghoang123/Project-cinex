package com.cinex.cinema.admin;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Kiểm thử các hàm phân tích văn bản (không dùng regex backtracking) của {@link AdminAiService}.
 */
class AdminAiTextParsingTest {

    private static String call(String method, Object... args) {
        return ReflectionTestUtils.invokeMethod(AdminAiService.class, method, args);
    }

    @Test
    void extractMovieTitleHandlesQuotedPlainAndStopMarkers() {
        assertThat(call("extractMovieTitleFromText", "Thêm phim \"Deadpool & Wolverine\" đang chiếu"))
            .isEqualTo("Deadpool & Wolverine");
        assertThat(call("extractMovieTitleFromText", "Thêm phim mới Dune Part Two đang chiếu, 166 phút"))
            .isEqualTo("Dune Part Two");
        assertThat(call("extractMovieTitleFromText", "Tạo phim Inside Out 2, thể loại hoạt hình"))
            .isEqualTo("Inside Out 2");
        assertThat(call("extractMovieTitleFromText", "xin chào")).isEmpty();
        assertThat(call("extractMovieTitleFromText", (Object) null)).isEmpty();
    }

    @Test
    void resolveRoomTokenPairIsBoundsSafe() {
        assertThat(call("resolveRoomTokenPair", new String[]{"Tạo", "phòng", "Screen", "3"}, 1))
            .isEqualTo("Screen 3");
        assertThat(call("resolveRoomTokenPair", new String[]{"phòng", "5"}, 0)).isEqualTo("Screen 5");
        assertThat(call("resolveRoomTokenPair", new String[]{"phòng", "IMAX,"}, 0)).isEqualTo("IMAX");
        assertThat(call("resolveRoomTokenPair", new String[]{"phòng", "có"}, 0)).isEmpty();
        assertThat(call("resolveRoomTokenPair", new String[]{"phòng", "screen"}, 0)).isEmpty();
        // Phần tử cuối mảng: không được ném ArrayIndexOutOfBoundsException
        assertThat(call("resolveRoomTokenPair", new String[]{"screen"}, 0)).isEmpty();
    }

    @Test
    void stripJsonBlocksRemovesFencedAndInlineActions() {
        AdminAiService service = new AdminAiService(
            null, null, null, null, null, null, null, null, null, null, null);
        String reply = "Đã xong.\n```json\n{\"actions\":[{\"type\":\"CREATE_MOVIE\"}]}\n```\nCảm ơn {\"actions\": [{\"a\":\"}\"}]} nhé";
        String stripped = ReflectionTestUtils.invokeMethod(service, "stripJsonBlocks", reply);
        assertThat(stripped).doesNotContain("actions").contains("Đã xong.").contains("Cảm ơn").endsWith("nhé");
    }
}
