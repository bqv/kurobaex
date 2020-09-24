package com.github.k1rakishou.model.mapper

import com.github.k1rakishou.model.data.descriptor.ChanDescriptor
import com.github.k1rakishou.model.data.descriptor.PostDescriptor
import com.github.k1rakishou.model.data.post.ChanPost
import com.github.k1rakishou.model.data.serializable.spans.SerializableSpannableString
import com.github.k1rakishou.model.entity.chan.post.ChanPostFull
import com.github.k1rakishou.model.entity.chan.post.ChanTextSpanEntity
import com.github.k1rakishou.model.entity.chan.thread.ChanThreadEntity
import com.google.gson.Gson

object ChanThreadMapper {

  fun toEntity(threadNo: Long, ownerBoardId: Long, chanPost: ChanPost): ChanThreadEntity {
    return ChanThreadEntity(
      threadId = 0L,
      threadNo = threadNo,
      ownerBoardId = ownerBoardId,
      lastModified = chanPost.lastModified,
      replies = chanPost.replies,
      threadImagesCount = chanPost.threadImagesCount,
      uniqueIps = chanPost.uniqueIps,
      sticky = chanPost.sticky,
      closed = chanPost.closed,
      archived = chanPost.archived
    )
  }

  fun fromEntity(
    gson: Gson,
    chanDescriptor: ChanDescriptor,
    chanThreadEntity: ChanThreadEntity,
    chanPostFull: ChanPostFull,
    chanTextSpanEntityList: List<ChanTextSpanEntity>?
  ): ChanPost {
    val postComment = TextSpanMapper.fromEntity(
      gson,
      chanTextSpanEntityList,
      ChanTextSpanEntity.TextType.PostComment
    ) ?: SerializableSpannableString()

    val subject = TextSpanMapper.fromEntity(
      gson,
      chanTextSpanEntityList,
      ChanTextSpanEntity.TextType.Subject
    ) ?: SerializableSpannableString()

    val tripcode = TextSpanMapper.fromEntity(
      gson,
      chanTextSpanEntityList,
      ChanTextSpanEntity.TextType.Tripcode
    ) ?: SerializableSpannableString()

    val postDescriptor = when (chanDescriptor) {
      is ChanDescriptor.ThreadDescriptor -> PostDescriptor.create(
        chanDescriptor.siteName(),
        chanDescriptor.boardCode(),
        chanDescriptor.threadNo,
        chanPostFull.chanPostIdEntity.postNo
      )
      is ChanDescriptor.CatalogDescriptor -> PostDescriptor.create(
        chanDescriptor.siteName(),
        chanDescriptor.boardCode(),
        chanPostFull.chanPostIdEntity.postNo
      )
    }

    return ChanPost(
      chanPostId = chanPostFull.chanPostIdEntity.postId,
      postDescriptor = postDescriptor,
      postImages = mutableListOf(),
      postIcons = mutableListOf(),
      replies = chanThreadEntity.replies,
      threadImagesCount = chanThreadEntity.threadImagesCount,
      uniqueIps = chanThreadEntity.uniqueIps,
      lastModified = chanThreadEntity.lastModified,
      sticky = chanThreadEntity.sticky,
      closed = chanThreadEntity.closed,
      deleted = chanPostFull.chanPostEntity.deleted,
      archiveId = chanPostFull.chanPostIdEntity.ownerArchiveId,
      archived = chanThreadEntity.archived,
      timestamp = chanPostFull.chanPostEntity.timestamp,
      name = chanPostFull.chanPostEntity.name,
      postComment = postComment,
      subject = subject,
      tripcode = tripcode,
      posterId = chanPostFull.chanPostEntity.posterId,
      moderatorCapcode = chanPostFull.chanPostEntity.moderatorCapcode,
      isOp = chanPostFull.chanPostEntity.isOp,
      isSavedReply = chanPostFull.chanPostEntity.isSavedReply
    )
  }

}