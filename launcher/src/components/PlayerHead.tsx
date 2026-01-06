import { useState } from 'react';

interface PlayerHeadProps {
  uuid: string;
  name: string;
  size: number;
  className?: string;
}

export default function PlayerHead({ uuid, name, size, className = '' }: PlayerHeadProps) {
  const [imageError, setImageError] = useState(false);

  // Remove dashes from UUID if present - Minecraft APIs need UUIDs without dashes
  const cleanUuid = uuid.replace(/-/g, '');

  // Use mc-heads.net which is very reliable and used by many launchers
  const avatarUrl = `https://mc-heads.net/avatar/${cleanUuid}/${size}`;

  if (imageError) {
    // Fallback to gradient box with first letter
    return (
      <div className={`bg-gradient-to-br from-miracle-400 to-miracle-600 flex items-center justify-center font-bold ${className}`}>
        {name?.[0]?.toUpperCase() || '?'}
      </div>
    );
  }

  return (
    <img
      src={avatarUrl}
      alt={name}
      className={className}
      onError={() => setImageError(true)}
      loading="lazy"
    />
  );
}
