import { useState, useEffect } from 'react';
import { useAuthStore } from '@/lib/stores/authStore';
import { useFriendsStore, Friend, User } from '@/lib/stores/friendsStore';
import PlayerHead from '@/components/PlayerHead';
import { Search, UserPlus, Check, X, Clock, Trash2, Users, Loader2 } from 'lucide-react';
import clsx from 'clsx';

export default function FriendsPage() {
  const { profile } = useAuthStore();
  const {
    friends,
    searchResults,
    isLoading,
    isSearching,
    registerUser,
    fetchFriends,
    searchUsers,
    sendFriendRequest,
    acceptRequest,
    removeFriend,
    clearSearch,
  } = useFriendsStore();

  const [searchQuery, setSearchQuery] = useState('');
  const [activeTab, setActiveTab] = useState<'friends' | 'requests' | 'add'>('friends');

  // Register user and fetch friends on mount
  useEffect(() => {
    if (profile?.id && profile?.name) {
      registerUser(profile.id, profile.name).then(() => {
        fetchFriends(profile.id);
      });
    }
  }, [profile?.id, profile?.name]);

  // Search with debounce
  useEffect(() => {
    const timer = setTimeout(() => {
      if (searchQuery && profile?.id) {
        searchUsers(searchQuery, profile.id);
      } else {
        clearSearch();
      }
    }, 300);

    return () => clearTimeout(timer);
  }, [searchQuery, profile?.id]);

  // Filter friends by status
  const acceptedFriends = friends.filter((f) => f.status === 'accepted');
  const incomingRequests = friends.filter((f) => f.status === 'pending' && f.is_incoming);
  const outgoingRequests = friends.filter((f) => f.status === 'pending' && !f.is_incoming);

  const handleSendRequest = async (user: User) => {
    if (!profile?.id || !profile?.name) return;
    const result = await sendFriendRequest(profile.id, profile.name, user.id);
    if (result.success) {
      setSearchQuery('');
      clearSearch();
    }
  };

  const handleAccept = async (friendshipId: string) => {
    const result = await acceptRequest(friendshipId);
    if (result.success && profile?.id) {
      fetchFriends(profile.id);
    }
  };

  const handleRemove = async (friendshipId: string) => {
    const result = await removeFriend(friendshipId);
    if (result.success && profile?.id) {
      fetchFriends(profile.id);
    }
  };

  const totalRequests = incomingRequests.length;

  return (
    <div className="h-full flex flex-col p-6 overflow-hidden">
      {/* Header */}
      <div className="mb-6 flex-shrink-0">
        <h1 className="text-2xl font-bold">
          <span className="gradient-text">Friends</span>
        </h1>
        <p className="text-theme-muted mt-1">
          {acceptedFriends.length} friend{acceptedFriends.length !== 1 ? 's' : ''}
        </p>
      </div>

      {/* Tabs */}
      <div className="flex gap-2 mb-6 flex-shrink-0">
        <button
          onClick={() => setActiveTab('friends')}
          className={clsx(
            'px-4 py-2 rounded-lg font-medium transition-colors flex items-center gap-2',
            activeTab === 'friends'
              ? 'bg-miracle-600 text-white'
              : 'bg-surface-secondary/30 text-theme-secondary hover:bg-surface-secondary/50'
          )}
        >
          <Users className="w-4 h-4" />
          Friends
        </button>
        <button
          onClick={() => setActiveTab('requests')}
          className={clsx(
            'px-4 py-2 rounded-lg font-medium transition-colors flex items-center gap-2 relative',
            activeTab === 'requests'
              ? 'bg-miracle-600 text-white'
              : 'bg-surface-secondary/30 text-theme-secondary hover:bg-surface-secondary/50'
          )}
        >
          <Clock className="w-4 h-4" />
          Requests
          {totalRequests > 0 && (
            <span className="absolute -top-1 -right-1 bg-red-500 text-white text-xs w-5 h-5 rounded-full flex items-center justify-center">
              {totalRequests}
            </span>
          )}
        </button>
        <button
          onClick={() => setActiveTab('add')}
          className={clsx(
            'px-4 py-2 rounded-lg font-medium transition-colors flex items-center gap-2',
            activeTab === 'add'
              ? 'bg-miracle-600 text-white'
              : 'bg-surface-secondary/30 text-theme-secondary hover:bg-surface-secondary/50'
          )}
        >
          <UserPlus className="w-4 h-4" />
          Add Friend
        </button>
      </div>

      {/* Content */}
      <div className="flex-1 overflow-y-auto">
        {isLoading ? (
          <div className="flex items-center justify-center h-full">
            <Loader2 className="w-8 h-8 animate-spin text-miracle-500" />
          </div>
        ) : (
          <>
            {/* Friends List */}
            {activeTab === 'friends' && (
              <div className="space-y-2">
                {acceptedFriends.length === 0 ? (
                  <div className="text-center py-12">
                    <Users className="w-12 h-12 mx-auto text-theme-muted mb-4" />
                    <p className="text-theme-muted">No friends yet</p>
                    <p className="text-theme-muted text-sm mt-1">
                      Add friends to see them here
                    </p>
                  </div>
                ) : (
                  acceptedFriends.map((friend) => (
                    <FriendCard
                      key={friend.friendship_id}
                      friend={friend}
                      onRemove={() => handleRemove(friend.friendship_id)}
                    />
                  ))
                )}
              </div>
            )}

            {/* Requests */}
            {activeTab === 'requests' && (
              <div className="space-y-6">
                {/* Incoming Requests */}
                <div>
                  <h3 className="text-sm font-semibold text-theme-muted mb-3">
                    INCOMING REQUESTS ({incomingRequests.length})
                  </h3>
                  {incomingRequests.length === 0 ? (
                    <p className="text-theme-muted text-sm">No incoming requests</p>
                  ) : (
                    <div className="space-y-2">
                      {incomingRequests.map((friend) => (
                        <RequestCard
                          key={friend.friendship_id}
                          friend={friend}
                          onAccept={() => handleAccept(friend.friendship_id)}
                          onDecline={() => handleRemove(friend.friendship_id)}
                        />
                      ))}
                    </div>
                  )}
                </div>

                {/* Outgoing Requests */}
                <div>
                  <h3 className="text-sm font-semibold text-theme-muted mb-3">
                    SENT REQUESTS ({outgoingRequests.length})
                  </h3>
                  {outgoingRequests.length === 0 ? (
                    <p className="text-theme-muted text-sm">No pending requests</p>
                  ) : (
                    <div className="space-y-2">
                      {outgoingRequests.map((friend) => (
                        <div
                          key={friend.friendship_id}
                          className="glass rounded-lg border border-theme-muted/20/50 p-3 flex items-center gap-3"
                        >
                          <PlayerHead
                            uuid={friend.user.minecraft_uuid}
                            name={friend.user.username}
                            size={40}
                            className="w-10 h-10 rounded-lg"
                          />
                          <div className="flex-1">
                            <div className="font-medium">{friend.user.username}</div>
                            <div className="text-xs text-theme-muted">Pending...</div>
                          </div>
                          <button
                            onClick={() => handleRemove(friend.friendship_id)}
                            className="p-2 text-theme-muted hover:text-red-400 hover:bg-surface-secondary/50 rounded-lg transition-colors"
                            title="Cancel request"
                          >
                            <X className="w-4 h-4" />
                          </button>
                        </div>
                      ))}
                    </div>
                  )}
                </div>
              </div>
            )}

            {/* Add Friend */}
            {activeTab === 'add' && (
              <div className="space-y-4">
                {/* Search Input */}
                <div className="relative">
                  <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-theme-muted" />
                  <input
                    type="text"
                    value={searchQuery}
                    onChange={(e) => setSearchQuery(e.target.value)}
                    placeholder="Search by username..."
                    className="w-full pl-10 pr-4 py-3 bg-surface-secondary/50 border border-theme-muted/20/50 rounded-lg focus:outline-none focus:border-miracle-500/50 transition-colors"
                    autoFocus
                  />
                  {isSearching && (
                    <Loader2 className="absolute right-3 top-1/2 -translate-y-1/2 w-5 h-5 animate-spin text-miracle-500" />
                  )}
                </div>

                {/* Search Results */}
                <div className="space-y-2">
                  {searchQuery.length > 0 && searchQuery.length < 2 && (
                    <p className="text-theme-muted text-sm text-center py-4">
                      Type at least 2 characters to search
                    </p>
                  )}

                  {searchQuery.length >= 2 && searchResults.length === 0 && !isSearching && (
                    <p className="text-theme-muted text-sm text-center py-4">
                      No users found matching "{searchQuery}"
                    </p>
                  )}

                  {searchResults.map((user) => {
                    // Check if already friends or request pending
                    const existingFriend = friends.find(
                      (f) => f.user.id === user.id
                    );
                    const isPending = existingFriend?.status === 'pending';
                    const isFriend = existingFriend?.status === 'accepted';

                    return (
                      <div
                        key={user.id}
                        className="glass rounded-lg border border-theme-muted/20/50 p-3 flex items-center gap-3"
                      >
                        <PlayerHead
                          uuid={user.minecraft_uuid}
                          name={user.username}
                          size={40}
                          className="w-10 h-10 rounded-lg"
                        />
                        <div className="flex-1">
                          <div className="font-medium">{user.username}</div>
                          {user.is_online && (
                            <div className="text-xs text-green-400">Online</div>
                          )}
                        </div>
                        {isFriend ? (
                          <span className="text-xs text-green-400 px-2 py-1 bg-green-500/20 rounded">
                            Friends
                          </span>
                        ) : isPending ? (
                          <span className="text-xs text-yellow-400 px-2 py-1 bg-yellow-500/20 rounded">
                            Pending
                          </span>
                        ) : (
                          <button
                            onClick={() => handleSendRequest(user)}
                            className="p-2 text-miracle-400 hover:text-miracle-300 hover:bg-miracle-500/20 rounded-lg transition-colors"
                            title="Send friend request"
                          >
                            <UserPlus className="w-5 h-5" />
                          </button>
                        )}
                      </div>
                    );
                  })}
                </div>
              </div>
            )}
          </>
        )}
      </div>
    </div>
  );
}

function FriendCard({ friend, onRemove }: { friend: Friend; onRemove: () => void }) {
  const isOnline = friend.user.is_online;

  return (
    <div className="glass rounded-lg border border-theme-muted/20/50 p-4 flex items-center gap-4 hover:border-miracle-500/30 transition-colors">
      <div className="relative">
        <PlayerHead
          uuid={friend.user.minecraft_uuid}
          name={friend.user.username}
          size={48}
          className="w-12 h-12 rounded-lg"
        />
        <div
          className={clsx(
            'absolute -bottom-1 -right-1 w-4 h-4 rounded-full border-2 border-surface-primary',
            isOnline ? 'bg-green-500' : 'bg-surface-secondary'
          )}
        />
      </div>

      <div className="flex-1">
        <div className="font-semibold">{friend.user.username}</div>
        <div className={clsx('text-sm', isOnline ? 'text-green-400' : 'text-theme-muted')}>
          {isOnline
            ? friend.user.current_server
              ? `Playing on ${friend.user.current_server}`
              : 'Online'
            : 'Offline'}
        </div>
      </div>

      <button
        onClick={onRemove}
        className="p-2 text-theme-muted hover:text-red-400 hover:bg-surface-secondary/50 rounded-lg transition-colors"
        title="Remove friend"
      >
        <Trash2 className="w-4 h-4" />
      </button>
    </div>
  );
}

function RequestCard({
  friend,
  onAccept,
  onDecline,
}: {
  friend: Friend;
  onAccept: () => void;
  onDecline: () => void;
}) {
  return (
    <div className="glass rounded-lg border border-theme-muted/20/50 p-3 flex items-center gap-3">
      <PlayerHead
        uuid={friend.user.minecraft_uuid}
        name={friend.user.username}
        size={40}
        className="w-10 h-10 rounded-lg"
      />
      <div className="flex-1">
        <div className="font-medium">{friend.user.username}</div>
        <div className="text-xs text-theme-muted">Wants to be your friend</div>
      </div>
      <div className="flex gap-2">
        <button
          onClick={onAccept}
          className="p-2 text-green-400 hover:bg-green-500/20 rounded-lg transition-colors"
          title="Accept"
        >
          <Check className="w-5 h-5" />
        </button>
        <button
          onClick={onDecline}
          className="p-2 text-red-400 hover:bg-red-500/20 rounded-lg transition-colors"
          title="Decline"
        >
          <X className="w-5 h-5" />
        </button>
      </div>
    </div>
  );
}
