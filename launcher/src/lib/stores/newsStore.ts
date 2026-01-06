import { create } from 'zustand';
import { persist } from 'zustand/middleware';

export interface NewsItem {
  id: string;
  title: string;
  date: string;
  content: string;
}

interface NewsState {
  newsItems: NewsItem[];
  lastSeenNewsId: string | null;
  hasUnreadNews: boolean;
  addNews: (news: Omit<NewsItem, 'id'>) => void;
  updateNews: (id: string, news: Partial<NewsItem>) => void;
  deleteNews: (id: string) => void;
  markAllAsRead: () => void;
}

export const useNewsStore = create<NewsState>()(
  persist(
    (set, get) => ({
      newsItems: [
        {
          id: '1',
          title: 'Miracle Client 1.0 Released!',
          date: 'December 2024',
          content: "We're excited to announce the first release of Miracle Client for Minecraft 1.21!",
        },
        {
          id: '2',
          title: 'Sodium & Iris Integration',
          date: 'December 2024',
          content: 'Experience incredible performance with built-in Sodium optimization and Iris shader support.',
        },
      ],
      lastSeenNewsId: null,
      hasUnreadNews: true,

      addNews: (news) =>
        set((state) => ({
          newsItems: [
            {
              ...news,
              id: Date.now().toString(),
            },
            ...state.newsItems,
          ],
          hasUnreadNews: true,
        })),

      updateNews: (id, news) =>
        set((state) => ({
          newsItems: state.newsItems.map((item) =>
            item.id === id ? { ...item, ...news } : item
          ),
        })),

      deleteNews: (id) =>
        set((state) => ({
          newsItems: state.newsItems.filter((item) => item.id !== id),
        })),

      markAllAsRead: () => {
        const { newsItems } = get();
        const latestId = newsItems.length > 0 ? newsItems[0].id : null;
        set({
          lastSeenNewsId: latestId,
          hasUnreadNews: false,
        });
      },
    }),
    {
      name: 'miracle-news',
    }
  )
);
