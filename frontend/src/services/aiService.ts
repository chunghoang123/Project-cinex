import api from '../api/axios';

export interface AiChatMessage {
  role: 'user' | 'assistant';
  content: string;
}

export interface AiMovieSuggestion {
  movieId: number;
  title: string;
  posterUrl: string;
  genre: string;
}

export interface AiChatResult {
  reply: string;
  model: string;
  movies?: AiMovieSuggestion[];
}

export const aiService = {
  /** Hỏi box AI. Toàn bộ câu trả lời do backend gọi NVIDIA sinh ra, không có fallback cục bộ. */
  chat: async (message: string, history: AiChatMessage[] = []): Promise<AiChatResult> => {
    const res = await api.post(
      '/ai/chat',
      { message, history: history.slice(-10) },
      { timeout: 60000 },
    );
    return res.data?.data as AiChatResult;
  },

  describeImage: async (file: File, prompt?: string) => {
    const formData = new FormData();
    formData.append('file', file);
    if (prompt) formData.append('prompt', prompt);

    const res = await api.post('/ai/describe-image', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
      timeout: 60000,
    });
    return res.data?.data;
  },
};
