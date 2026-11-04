import api from '../api/axios';

export const aiService = {
  describeImage: async (file: File, prompt?: string) => {
    const formData = new FormData();
    formData.append('file', file);
    if (prompt) formData.append('prompt', prompt);

    const res = await api.post('/ai/describe-image', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    });
    return res.data;
  },
};
