import { useState } from 'react';
import { X, Plus, Edit2, Trash2 } from 'lucide-react';
import { useNewsStore, NewsItem } from '@/lib/stores/newsStore';
import { useModal } from '@/components/ui';

interface NewsManagementModalProps {
  onClose: () => void;
}

export default function NewsManagementModal({ onClose }: NewsManagementModalProps) {
  const modal = useModal();
  const { newsItems, addNews, updateNews, deleteNews } = useNewsStore();
  const [editingId, setEditingId] = useState<string | null>(null);
  const [formData, setFormData] = useState({
    title: '',
    date: '',
    content: '',
  });

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();

    if (!formData.title || !formData.date || !formData.content) {
      return;
    }

    if (editingId) {
      updateNews(editingId, formData);
      setEditingId(null);
    } else {
      addNews(formData);
    }

    setFormData({ title: '', date: '', content: '' });
  };

  const handleEdit = (item: NewsItem) => {
    setEditingId(item.id);
    setFormData({
      title: item.title,
      date: item.date,
      content: item.content,
    });
  };

  const handleCancel = () => {
    setEditingId(null);
    setFormData({ title: '', date: '', content: '' });
  };

  const handleDelete = async (id: string) => {
    const confirmed = await modal.confirm({
      title: 'Delete News Item',
      message: 'Are you sure you want to delete this news item?',
      confirmLabel: 'Delete',
      variant: 'danger',
    });

    if (confirmed) {
      deleteNews(id);
    }
  };

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm"
      onClick={onClose}
    >
      <div
        className="glass rounded-2xl border border-theme-muted/20/50 shadow-2xl w-[70vw] h-[70vh] flex flex-col overflow-hidden"
        onClick={(e) => e.stopPropagation()}
      >
        {/* Header */}
        <div className="p-6 border-b border-theme-muted/20/50 flex items-center justify-between flex-shrink-0">
          <h2 className="text-2xl font-bold gradient-text">Manage News & Updates</h2>
          <button
            onClick={onClose}
            className="p-2 hover:bg-surface-secondary/50 rounded-lg transition-colors"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Content */}
        <div className="flex-1 overflow-y-auto p-6">
          <div className="grid grid-cols-2 gap-6">
            {/* Form */}
            <div>
              <h3 className="text-lg font-semibold mb-4">
                {editingId ? 'Edit News Item' : 'Add News Item'}
              </h3>
              <form onSubmit={handleSubmit} className="space-y-4">
                <div>
                  <label className="block text-sm font-medium text-theme-secondary mb-2">
                    Title
                  </label>
                  <input
                    type="text"
                    value={formData.title}
                    onChange={(e) => setFormData({ ...formData, title: e.target.value })}
                    className="w-full px-4 py-2.5 bg-surface-secondary/50 border border-theme-muted/20/50 rounded-lg focus:outline-none focus:border-miracle-500 focus:bg-surface-secondary transition-all text-theme-primary placeholder:text-theme-muted"
                    placeholder="Enter news title..."
                    required
                  />
                </div>

                <div>
                  <label className="block text-sm font-medium text-theme-secondary mb-2">
                    Date
                  </label>
                  <input
                    type="text"
                    value={formData.date}
                    onChange={(e) => setFormData({ ...formData, date: e.target.value })}
                    className="w-full px-4 py-2.5 bg-surface-secondary/50 border border-theme-muted/20/50 rounded-lg focus:outline-none focus:border-miracle-500 focus:bg-surface-secondary transition-all text-theme-primary placeholder:text-theme-muted"
                    placeholder="e.g., December 2024"
                    required
                  />
                </div>

                <div>
                  <label className="block text-sm font-medium text-theme-secondary mb-2">
                    Content
                  </label>
                  <textarea
                    value={formData.content}
                    onChange={(e) => setFormData({ ...formData, content: e.target.value })}
                    className="w-full px-4 py-2.5 bg-surface-secondary/50 border border-theme-muted/20/50 rounded-lg focus:outline-none focus:border-miracle-500 focus:bg-surface-secondary transition-all resize-none text-theme-primary placeholder:text-theme-muted"
                    placeholder="Enter news content..."
                    rows={6}
                    required
                  />
                </div>

                <div className="flex gap-3">
                  <button
                    type="submit"
                    className="flex-1 px-6 py-2.5 rounded-lg font-medium transition-all bg-gradient-to-r from-miracle-500 to-miracle-600 hover:from-miracle-400 hover:to-miracle-500 text-theme-primary"
                  >
                    <Plus className="w-4 h-4 inline-block mr-2" />
                    {editingId ? 'Update' : 'Add News'}
                  </button>
                  {editingId && (
                    <button
                      type="button"
                      onClick={handleCancel}
                      className="px-6 py-2.5 rounded-lg font-medium transition-all bg-surface-secondary hover:bg-miracle-500/20 text-theme-primary"
                    >
                      Cancel
                    </button>
                  )}
                </div>
              </form>
            </div>

            {/* News List */}
            <div>
              <h3 className="text-lg font-semibold mb-4">Current News Items</h3>
              <div className="space-y-3">
                {newsItems.map((item) => (
                  <div
                    key={item.id}
                    className="p-4 bg-surface-secondary/50 rounded-lg border border-theme-muted/20/30"
                  >
                    <div className="flex justify-between items-start mb-2">
                      <h4 className="font-semibold">{item.title}</h4>
                      <div className="flex gap-2">
                        <button
                          onClick={() => handleEdit(item)}
                          className="p-1 hover:bg-surface-secondary rounded transition-colors"
                        >
                          <Edit2 className="w-4 h-4 text-miracle-400" />
                        </button>
                        <button
                          onClick={() => handleDelete(item.id)}
                          className="p-1 hover:bg-surface-secondary rounded transition-colors"
                        >
                          <Trash2 className="w-4 h-4 text-red-400" />
                        </button>
                      </div>
                    </div>
                    <span className="text-xs text-theme-muted block mb-2">{item.date}</span>
                    <p className="text-sm text-theme-muted">{item.content}</p>
                  </div>
                ))}
                {newsItems.length === 0 && (
                  <div className="text-center text-theme-muted py-8">
                    No news items yet. Add one to get started!
                  </div>
                )}
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
