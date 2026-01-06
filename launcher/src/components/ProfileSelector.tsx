import { useState, useEffect, useRef } from 'react';
import { ChevronDown, Settings, Folder } from 'lucide-react';
import clsx from 'clsx';
import { useProfileStore, Profile } from '@/lib/stores/profileStore';

interface ProfileSelectorProps {
  selectedVersion: string;
  onManageProfiles: () => void;
}

export default function ProfileSelector({ selectedVersion, onManageProfiles }: ProfileSelectorProps) {
  const [isOpen, setIsOpen] = useState(false);
  const dropdownRef = useRef<HTMLDivElement>(null);

  const {
    profiles,
    activeProfileId,
    isLoading,
    fetchProfiles,
    fetchActiveProfile,
    setActiveProfile,
  } = useProfileStore();

  // Fetch profiles when version changes
  useEffect(() => {
    fetchProfiles(selectedVersion);
    fetchActiveProfile(selectedVersion);
  }, [selectedVersion, fetchProfiles, fetchActiveProfile]);

  // Close dropdown when clicking outside
  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target as Node)) {
        setIsOpen(false);
      }
    };

    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  const activeProfile = profiles.find(p => p.id === activeProfileId);
  const versionProfiles = profiles.filter(p => p.version === selectedVersion);

  const handleSelectProfile = async (profile: Profile) => {
    try {
      await setActiveProfile(selectedVersion, profile.id);
      setIsOpen(false);
    } catch (error) {
      console.error('Failed to switch profile:', error);
    }
  };

  const getProfileLabel = (profile: Profile) => {
    if (profile.is_preset && profile.preset_type) {
      return `${profile.name} (Preset)`;
    }
    if (profile.is_default) {
      return `${profile.name} (Default)`;
    }
    return profile.name;
  };

  return (
    <div ref={dropdownRef} className="relative">
      {/* Profile Selector Button */}
      <button
        onClick={() => setIsOpen(!isOpen)}
        className={clsx(
          'w-full rounded-lg px-4 py-3 flex items-center justify-between',
          'hover:bg-miracle-500/10 transition-colors',
          isLoading && 'opacity-50 cursor-wait'
        )}
        disabled={isLoading}
      >
        <div className="flex items-center gap-3">
          <Folder className="w-4 h-4 text-miracle-500" />
          <div className="text-left">
            <div className="text-xs text-theme-muted">Profile</div>
            <div className="font-medium text-sm text-theme-primary">
              {activeProfile ? getProfileLabel(activeProfile) : 'Default'}
            </div>
          </div>
        </div>
        <ChevronDown
          className={clsx(
            'w-4 h-4 text-theme-muted transition-transform',
            isOpen && 'rotate-180'
          )}
        />
      </button>

      {/* Dropdown */}
      {isOpen && (
        <div className="absolute top-full left-0 right-0 mt-2 z-50 glass rounded-lg shadow-xl overflow-hidden">
          {/* Profile List */}
          <div className="max-h-48 overflow-y-auto">
            {versionProfiles.length === 0 ? (
              <div className="px-4 py-3 text-sm text-theme-muted">
                No profiles for this version
              </div>
            ) : (
              versionProfiles.map((profile) => (
                <button
                  key={profile.id}
                  onClick={() => handleSelectProfile(profile)}
                  className={clsx(
                    'w-full px-4 py-3 text-left text-sm text-theme-primary hover:bg-miracle-500/10 transition-colors flex items-center justify-between',
                    profile.id === activeProfileId && 'bg-miracle-600/20'
                  )}
                >
                  <span>{getProfileLabel(profile)}</span>
                  {profile.is_preset && (
                    <span className="text-xs px-2 py-0.5 rounded bg-miracle-500/20 text-miracle-500">
                      {profile.preset_type}
                    </span>
                  )}
                </button>
              ))
            )}
          </div>

          {/* Divider */}
          <div className="border-t border-theme-muted/20" />

          {/* Actions */}
          <div className="p-2">
            <button
              onClick={() => {
                setIsOpen(false);
                onManageProfiles();
              }}
              className="w-full px-3 py-2 text-sm text-left text-theme-primary rounded-lg hover:bg-miracle-500/10 transition-colors flex items-center gap-2"
            >
              <Settings className="w-4 h-4" />
              Manage Profiles
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
