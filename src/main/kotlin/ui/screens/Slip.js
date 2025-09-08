import React, {
  useCallback,
  useEffect,
  useRef,
  useState,
  useMemo,
} from "react";
import { useLocation, useNavigate } from "react-router-dom";
import axios from "axios";
import Cookies from "universal-cookie";
import SlipDetail from "../../components/Slip/SlipDetail";
import Select from "react-select";
import useFetchData from "../../utils/hooks/useFetchData";
import { toast } from "react-toastify";
import { axiosPrivate } from "../../utils/constants/api/axiosPrivate";
import { useDispatch } from "react-redux";
import {
  closeModal,
  openModal,
  selectModalMode,
} from "../../redux/features/modalSlice";
import { useAppSelector } from "../../redux/config/hooks";
import Modal from "../../components/Modal";
import UpdateSlipSingle from "../../components/Form/UpdateSingleSlip";
import { selectSlipSingleUpdData } from "../../redux/features/SlipSingleUpdateSlice";
import {
  TbEdit,
  TbHome,
  TbRefresh,
  TbTrash,
  TbUserHexagon,
} from "react-icons/tb";
import ChangeSlipUserForm from "../../components/Form/ChangeSlipUserForm";
import { formatUserName } from "../../utils/helpers/untilFun";
import Swal from "sweetalert2";
import { getUserProfile } from "../../redux/features/userProfileSlice";
import { getTermList, getUserList } from "../../redux/features/tempDataSlice";
import {
  selectHomeTermId,
  selectHomeUserId,
  setHomeUserId,
} from "../../redux/features/homeSlice";
import { FaChartPie } from "react-icons/fa";
import saleIcon from "../../../assets/sale-icon.png";
import Input from "../../components/Form/Input";

const cookie = new Cookies();
const Slip = () => {
  const token = cookie.get("token");
  const location = useLocation();
  const navigate = useNavigate();
  const [termId, setTermId] = useState(location.state?.termId || ""); // default to ""
  const [userId, setUserId] = useState(location.state?.userId || "");
  // const [originalSlips, setOriginalSlips] = useState([]);
  // const [filteredSlips, setFilteredSlips] = useState([]);
  const [slips, setSlips] = useState([]);
  const [selectedSlip, setSelectedSlip] = useState(null);
  const [slipDetails, setSlipDetails] = useState(null);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(true);
  const [loadingDetail, setLoadingDetail] = useState(false);
  const [termOptions, setTermOptions] = useState([]);
  const [users, setUsers] = useState("");
  const [refersh, setRefersh] = useState("");
  const [slipRefresh, setSlipRefresh] = useState("");
  const [showAmount, setShowAmount] = useState(false);
  const [report, setReport] = useState(null);

  const [page, setPage] = useState(1);
  const [hasMore, setHasMore] = useState(true);
  const observer = useRef();
  const rawTermData = useAppSelector(getTermList);

  const tempTermData = useMemo(() => {
    return rawTermData.filter((term) => term.isFinished == 0);
  }, [rawTermData]);
  const dispatch = useDispatch();
  const tempUserData = useAppSelector(getUserList);
  const modalMode = useAppSelector(selectModalMode);
  const slipData = useAppSelector(selectSlipSingleUpdData);
  const userProfile = useAppSelector(getUserProfile);
  const homeTermId = useAppSelector(selectHomeTermId);
  const homeUserId = useAppSelector(selectHomeUserId);
  const initializeTermData = (terms) => {
    const termOptions = terms.map((term) => ({
      value: term.termId,
      label: term.termName,
    }));
    setTermOptions(termOptions);
    if (terms.length > 0) {
      setTermId(location.state?.termId || homeTermId || terms[0]?.termId);
    }
  };

  useEffect(() => {
    try {
      if (tempTermData && tempTermData.length > 0) {
        initializeTermData(tempTermData.filter((term) => term.isFinished == 0));
      }
    } catch (error) {
      console.log("Error fetching active terms:", error);
    }
  }, [tempTermData]);

  useEffect(() => {
    try {
      if (tempUserData && tempUserData.length > 0) {
        console.log("tempUserData => ", tempUserData);
        let tmpUser = tempUserData.map((user) => ({
          value: user.userId,
          label: formatUserName(user.name, user.userType),
          discount2D: user.discount2D,
          discount3D: user.discount3D,
          userType:user.userType
        }));
        tmpUser.unshift({ value: "", label: "All" });
        setUsers(tmpUser);
        if (tmpUser.length > 0) {
          const uid = homeUserId == 0 ? "" : homeUserId;
          setUserId(location.state?.userId || uid);
        }
      }
    } catch (error) {
      console.log("Error fetching active terms:", error);
    }
  }, [tempUserData]);

  // Fetch slips
  const fetchSlips = useCallback(
    async (ss) => {
      if (!termId && !userId) return;
      setLoading(true);

      try {
        const response = await axiosPrivate.get(
          `/v1/slip/getSlips?termId=${termId}&userId=${userId}&current=${page}&limit=30`,
          {
            headers: { Authorization: `Bearer ${token}` },
          }
        );

        if (response.data.code === "200") {
          const newSlips = response.data.data.by;
          setTotal(response.data.data.pagination.total);

          if (!selectedSlip && !ss) setSelectedSlip(newSlips[0]);
          setSlips((prev) => [...prev, ...newSlips]);
          setHasMore(newSlips.length > 0);
        } else {
          setHasMore(false);
        }
      } catch (error) {
        console.log("Error fetching slips:", error);
      } finally {
        setLoading(false);
      }
    },
    [termId, userId, page]
  );

  // Fetch slips
  const fetchStatements = useCallback(async () => {
    if (!termId && !userId) return;
    if (!showAmount) return;
    try {
      const response = await axiosPrivate.get(
        `/v1/report/getStatementByTermId?termId=${termId}&userId=${userId}`,
        {
          headers: { Authorization: `Bearer ${token}` },
        }
      );

      if (response.data.code === "200") {
        setReport(response.data.data);
      } else {
        setReport(null);
      }
    } catch (error) {
      console.log("Error fetching statements:", error);
    } finally {
      setLoading(false);
    }
  }, [termId, userId, showAmount]);
  useEffect(() => {
    if (termId || userId) {
      setHasMore(true);
      fetchSlips(selectedSlip || "");
    }
  }, [termId, slipRefresh, userId, page]);
  useEffect(() => {
    if (termId || userId) {
      fetchStatements();
    }
  }, [termId, slipRefresh, userId, showAmount]);
  const lastSlipRef = useCallback(
    (node) => {
      if (loading) return;
      if (observer.current) observer.current.disconnect();

      observer.current = new IntersectionObserver((entries) => {
        if (entries[0].isIntersecting && hasMore) {
          setPage(page + 1);
        }
      });

      if (node) observer.current.observe(node);
    },
    [loading, hasMore]
  );

  // Fetch slip details when a slip is selected
  useEffect(() => {
    const fetchSlipDetails = async () => {
      if (!selectedSlip) return;

      setLoadingDetail(true);
      try {
        const response = await axiosPrivate.get(
          `/v1/slip/getSlipDetail?slipId=${selectedSlip.slipId}&termId=${selectedSlip.termId}&userId=${selectedSlip.userId}`
        );

        if (response.data.code === "200") {
          setSlipDetails({
            smsCopy: response.data.data[0].smsCopy,
            copy: response.data.data[0].copy,
            slipNumber: selectedSlip.slipId,
            customerName: selectedSlip.user.name,
            userId: selectedSlip?.user.id,
            totalAmount: parseInt(selectedSlip.totalAmount, 10),
            phoneNumber: response.data.phoneNumber,
            items: response.data.data.map((item) => ({
              _id: item._id,
              number: item.number,
              type: item.type,
              amount: parseInt(item.amount, 10),
              slipId: item.slipId,
              termId: item.termId,
              userId: item.userId,
              showSummary: item.showSummary,
              summary: item.summary,
              groupId: item.groupId,
            })),
            status: selectedSlip.status,
          });
        } else {
          setSlipDetails(null);
          setSelectedSlip(null);
          setTotal(0);
          setSlips([]);
          setPage(1);
          setSlipRefresh(Math.random());
        }
      } catch (error) {
        console.log("Error fetching slip details:", error);
      } finally {
        setLoadingDetail(false);
      }
    };
    setTimeout(fetchSlipDetails, 0);
  }, [selectedSlip, refersh]);

  // Calculate total amount of all slips
  const totalAmountOfAllSlips = slips.reduce(
    (sum, slip) => sum + parseInt(slip.totalAmount, 10),
    0
  );
  function handleRefersh() {
    setSlips([]);
    setPage(1);
    // let tmpSelectd = selectedSlip
    setSelectedSlip(null);
    setSlipRefresh(Math.random());
    // setSelectedSlip(tmpSelectd)
  }

  const handleTermNameChange = (selectedOption) => {
    setSelectedSlip(null);
    setTotal(0);
    setSlips([]);
    setPage(1);
    setTermId(selectedOption.value);
  };

  const handleUserChange = (selectedOption) => {
    console.log("selectedOption => ", selectedOption);
    setSelectedSlip(null);
    setTotal(0);
    setSlips([]);
    setPage(1);
    // fetchSlips();
    setUserId(selectedOption.value);
    dispatch(setHomeUserId(selectedOption.value));
  };

  const handleDeleteSingleSlip = async (index, item) => {
    try {
      const currentSlipDetails = slipDetails;

      let currentGroupId = null;

      if (currentSlipDetails && currentSlipDetails.items) {
        const currentItemInDetails = currentSlipDetails.items.find(
          (slipItem) =>
            slipItem._id === item._id ||
            (slipItem.number === item.number && slipItem.amount === item.amount)
        );
        currentGroupId = currentItemInDetails?.groupId;
      }

      // Check if there are other items in the same group
      let itemsWithSameGroupId = [];
      if (currentGroupId && currentSlipDetails && currentSlipDetails.items) {
        itemsWithSameGroupId = currentSlipDetails.items.filter(
          (slipItem) =>
            slipItem.groupId === currentGroupId && slipItem._id !== item._id
        );
      }

      // If there are other items in the group, ask user for choice
      if (itemsWithSameGroupId.length > 0) {
        const result = await Swal.fire({
          title: "Delete Options",
          //text: `This item is part of a group with ${itemsWithSameGroupId.length} other items. What would you like to delete?`,
          icon: "question",
          showCancelButton: true,
          showDenyButton: true,
          confirmButtonText: "Delete One",
          denyButtonText: "Delete Group",
          cancelButtonText: "Cancel",
          confirmButtonColor: "#3085d6",
          denyButtonColor: "#d33",
          cancelButtonColor: "#6c757d",
        });

        if (result.isDenied) {
          // Delete all items in the group
          const allGroupItems = currentSlipDetails.items.filter(
            (slipItem) => slipItem.groupId === currentGroupId
          );

          // Show loading state in SweetAlert
          Swal.fire({
            title: "Deleting Group",
            text: `Deleting ${allGroupItems.length} items. Please wait...`,
            icon: "info",
            allowOutsideClick: false,
            allowEscapeKey: false,
            showConfirmButton: false,
            didOpen: () => {
              Swal.showLoading();
            }
          });

          // Delete each item in the group
          let successCount = 0;
          let errorCount = 0;
          
          for (const groupItem of allGroupItems) {
            try {
              await axiosPrivate.delete("/v1/slip/deleteSlip", {
                data: {
                  _id: groupItem._id,
                  slipId: groupItem.slipId,
                  termId: groupItem.termId,
                  userId: groupItem.userId,
                },
                headers: {
                  "Content-Type": "application/json",
                  Authorization: `Bearer ${token}`,
                },
              });
              successCount++;
            } catch (error) {
              console.log("Error deleting group item:", error);
              errorCount++;
            }
          }

          // Close the loading SweetAlert
          Swal.close();

          // Show result message
          if (errorCount === 0) {
            toast.success(`All ${successCount} group items deleted successfully`, { autoClose: 1000 });
          } else if (successCount === 0) {
            toast.error(`Failed to delete any items. ${errorCount} errors occurred.`, { autoClose: 1000 });
          } else {
            toast.warning(`${successCount} items deleted successfully, ${errorCount} failed.`, { autoClose: 1000 });
          }
          
          setRefersh(Math.random());
          return;
        } else if (result.isDismissed) {
          // User cancelled
          return;
        }
        // If confirmed, continue with single item deletion
      }

      // Delete single item and update remaining group items if needed
      if (currentGroupId && itemsWithSameGroupId.length > 0) {
        // Update remaining items in the group to have new groupId
        const ledgerItems = itemsWithSameGroupId.map((slipItem) => ({
          _id: slipItem._id || "",
          number: slipItem.number,
          amount: slipItem.amount,
          showSummary: "1",
          groupId: crypto.randomUUID
            ? crypto.randomUUID()
            : Date.now().toString() + Math.random().toString(36).substr(2, 9),
          summary: slipItem.number,
        }));

        const updatePayload = {
          termId: item.termId,
          userId: item.userId,
          slipId: item.slipId,
          ledger: ledgerItems,
        };

        await axiosPrivate.put("/v1/slip/updateSlips", updatePayload, {
          headers: {
            "Content-Type": "application/json",
            Authorization: `Bearer ${token}`,
          },
        });
      }

      // Delete the single item
      await axiosPrivate
        .delete("/v1/slip/deleteSlip", {
          data: {
            _id: item._id,
            slipId: item.slipId,
            termId: item.termId,
            userId: item.userId,
          },
          headers: {
            "Content-Type": "application/json",
            Authorization: `Bearer ${token}`,
          },
        })
        .then((response) => {
          toast.success(response.data?.message, { autoClose: 1000 });
          setRefersh(Math.random());
        })
        .catch((error) => {
          toast.error(error.response.data?.message, { autoClose: 1000 });
        });
    } catch (error) {
      console.log(error);
      toast.error("Error deleting slip:", { autoClose: 1000 });
    }
  };

  // Helper function to handle R pattern updates
  const handleRPatternUpdate = (originalNumber, amount, groupId) => {
    // Only handle 2-digit numbers for now
    if (originalNumber.length !== 2) {
      return [
        {
          number: originalNumber,
          amount: amount,
          showSummary: "1",
          groupId: groupId,
          summary: originalNumber,
        },
      ];
    }

    const reversedNumber = originalNumber.split("").reverse().join("");

    // // If the number is the same when reversed (like 11, 22, etc.), return single item
    // if (originalNumber === reversedNumber) {
    //   return [{
    //     number: originalNumber,
    //     amount: amount,
    //     showSummary: "1",
    //     groupId: groupId,
    //     summary: originalNumber
    //   }];
    // }

    // Return both original and reversed numbers
    return [
      {
        number: originalNumber,
        amount: amount,
        showSummary: "1",
        groupId: groupId,
        summary: `${originalNumber}R`,
      },
      {
        number: reversedNumber,
        amount: amount,
        showSummary: "0",
        groupId: groupId,
        summary: reversedNumber,
      },
    ];
  };

  // Helper function to handle NStar pattern updates
  const handleNStarPatternUpdate = (digit, amount, groupId) => {
    // Generate 10 numbers with the digit at the beginning (like 1* generates 10, 11, 12, etc.)
    return Array.from({ length: 10 }, (_, i) => ({
      number: `${digit}${i}`,
      amount: amount,
      showSummary: i === 0 ? "1" : "0",
      groupId: groupId,
      summary: `${digit} ထိပ်`,
    }));
  };

  // Helper function to handle Break pattern updates
  const handleBreakPatternUpdate = (endSum, amount, groupId) => {
    const entries = [];

    const digitsB = endSum.toString().split("").map(Number);
    const digitSumB = digitsB.reduce((sum, num) => sum + num, 0);
    for (let i = 0; i < 100; i++) {
      const digits = i.toString().split("").map(Number);
      const digitSum = digits.reduce((sum, num) => sum + num, 0);

      if (digitSum % 10 === digitSumB) {
        entries.push({
          number: i.toString().padStart(2, "0"),
          amount: amount,
          summary: `${i % 10}B`,
          showSummary: entries.length === 0 ? "1" : "0",
          groupId: groupId,
          delete: false,
        });
      }
    }

    return entries;
  };

  // Helper function to handle StarN pattern updates
  const handleStarNPatternUpdate = (digit, amount, groupId) => {
    // Generate 10 numbers with the digit at the end (like *1 generates 01, 11, 21, etc.)
    return Array.from({ length: 10 }, (_, i) => ({
      number: `${i}${digit}`,
      amount: amount,
      showSummary: i === 0 ? "1" : "0",
      groupId: groupId,
      summary: `${digit} ပိတ်`,
    }));
  };

  // Helper function to handle NP pattern updates
  const handleNPPatternUpdate = (digit, amount, groupId) => {
    const entries = [];

    for (let i = 0; i <= 99; i++) {
      const num = i.toString().padStart(2, "0"); // Ensure it's two digits
      if (num.includes(digit)) {
        entries.push({
          number: num,
          amount: amount,
          summary: digit + "ပါ",
          groupId: groupId,
          showSummary: entries.length === 0 ? "1" : "0",
          delete: false,
        });
      }
    }

    return entries;
  };

  // Helper function to handle DoubleNStar pattern updates
  const handleDoubleNStarPatternUpdate = (digit, amount, groupId) => {
    const entries = [];
    for (let i = 0; i < 10; i++) {
      const number = digit + i.toString(); // Generate numbers ending with the digit
      entries.push({
        number: number,
        amount: amount,
        summary: `${digit}*`,
        showSummary: entries.length === 0 ? "1" : "0",
        groupId: groupId,
        delete: false,
      });
    }
    return entries;
  };

  // Helper function to handle StartDoubleN pattern updates
  const handleStartDoubleNPatternUpdate = (digit, amount, groupId) => {
    const entries = [];
    for (let i = 0; i < 10; i++) {
      const number = `${i.toString()}${digit}`; // Generate numbers starting with the digit
      entries.push({
        number: number,
        amount: amount,
        summary: `*${digit}`,
        showSummary: entries.length === 0 ? "1" : "0",
        groupId: groupId,
        delete: false,
      });
    }
    return entries;
  };

  // Helper function to handle NStarN pattern updates
  const handleNStarNPatternUpdate = (digit, digit2, amount, groupId) => {
    const entries = [];
    for (let i = 0; i < 10; i++) {
      const number = `${digit}${i}${digit2}`; // Generate numbers matching *N*
      entries.push({
        number: number,
        amount: amount,
        summary: `${digit}*${digit2}`,
        showSummary: entries.length === 0 ? "1" : "0",
        groupId: groupId,
        delete: false,
      });
    }
    return entries;
  };

  // Helper function to handle 3-digit R pattern updates
  const handle3DRPatternUpdate = (digits, amount, groupId) => {
    const entries = [];

    // Function to generate all permutations of a string
    const getPermutations = (str) => {
      if (str.length <= 1) return [str];
      const permutations = [];
      const smallerPermutations = getPermutations(str.slice(1));
      for (const perm of smallerPermutations) {
        for (let i = 0; i <= perm.length; i++) {
          const newPerm = perm.slice(0, i) + str[0] + perm.slice(i);
          permutations.push(newPerm);
        }
      }
      return permutations;
    };

    // Generate unique permutations
    const permutations = [...new Set(getPermutations(digits))];

    // Create entries
    permutations.forEach((number, index) => {
      entries.push({
        number: number,
        amount: amount,
        summary: `${digits}R`,
        showSummary: index === 0 ? "1" : "0",
        groupId: groupId,
        delete: false,
      });
    });

    return entries;
  };

  const handleUpdateSingleSlip = async (number, amount) => {
    try {
      const currentSlipDetails = slipDetails;
      const currentItem = slipData;

      // Check if the same number is entered - do nothing if unchanged
      if (currentItem.number === number && currentItem.amount === amount) {
        dispatch(closeModal());
        return; // Exit the function without doing anything
      }

      // Check if only amount changed but number is the same
      const isOnlyAmountChanged =
        currentItem.number === number && currentItem.amount !== amount;
      if (isOnlyAmountChanged) {
        // For amount-only changes, we need to check if this item is part of a pattern group
        // and update all items in that group with the new amount
        let currentGroupId = null;

        if (currentSlipDetails && currentSlipDetails.items) {
          const currentItemInDetails = currentSlipDetails.items.find(
            (item) =>
              item._id === currentItem._id ||
              (item.number === currentItem.number &&
                item.amount === currentItem.amount)
          );
          currentGroupId = currentItemInDetails?.groupId;
        }

        if (currentGroupId && currentSlipDetails && currentSlipDetails.items) {
          const itemsWithSameGroupId = currentSlipDetails.items.filter(
            (item) => item.groupId === currentGroupId
          );

          // Check if this is a pattern group
          const hasRPattern = itemsWithSameGroupId.some(
            (item) => item.summary && item.summary.endsWith("R")
          );
          const has3DRPattern = itemsWithSameGroupId.some(
            (item) =>
              item.summary &&
              item.summary.endsWith("R") &&
              item.number &&
              item.number.length === 3
          );
          const hasNStarPattern = itemsWithSameGroupId.some(
            (item) => item.summary && item.summary.includes("ထိပ်")
          );
          const hasBreakPattern = itemsWithSameGroupId.some(
            (item) =>
              item.summary &&
              (item.summary.endsWith("B") || item.summary.includes("ဘရိတ်"))
          );
          const hasStarNPattern = itemsWithSameGroupId.some(
            (item) => item.summary && item.summary.includes("ပိတ်")
          );
          const hasNPPattern = itemsWithSameGroupId.some(
            (item) => item.summary && item.summary.includes("ပါ")
          );
          const hasDoubleNStarPattern = itemsWithSameGroupId.some(
            (item) => item.summary && /^\d\d\*$/.test(item.summary)
          );
          const hasStartDoubleNPattern = itemsWithSameGroupId.some(
            (item) => item.summary && /^\*\d\d$/.test(item.summary)
          );
          const hasNStarNPattern = itemsWithSameGroupId.some(
            (item) => item.summary && /^\d\*\d$/.test(item.summary)
          );

          // If it's a pattern group, update all items in the group with new amount
          if (itemsWithSameGroupId.length > 1 && 
              (hasRPattern || has3DRPattern || hasNStarPattern || hasBreakPattern || 
               hasStarNPattern || hasNPPattern || hasDoubleNStarPattern || 
               hasStartDoubleNPattern || hasNStarNPattern)) {
            
            // Update all items in the group with the new amount
            const updatedGroupItems = itemsWithSameGroupId.map((item) => ({
              _id: item._id || "",
              number: item.number,
              amount: amount, // Update amount for all items in the group
              showSummary: item.showSummary,
              groupId: currentGroupId,
              summary: item.summary,
            }));

            // Get all items from slip details that are NOT in the current group
            const otherItems = currentSlipDetails.items.filter(
              (item) => item.groupId !== currentGroupId
            );

            const payload = {
              termId: slipData.termId,
              userId: slipData.userId,
              slipId: slipData.slipId,
              ledger: [...otherItems, ...updatedGroupItems],
            };

            const response = await axiosPrivate.put(
              "/v1/slip/updateSlips",
              payload,
              {
                headers: {
                  "Content-Type": "application/json",
                  Authorization: `Bearer ${token}`,
                },
              }
            );

            if (response.data.code === "200") {
              toast.success(response.data?.message, { autoClose: 1000 });
              dispatch(closeModal());
              setRefersh(Math.random());
            } else {
              toast.error(response.data?.message, { autoClose: 1000 });
              setRefersh(Math.random());
            }
            dispatch(closeModal());
            return; // Exit early for pattern group amount changes
          } else {
            // For single items or non-pattern groups, just update the current item
            const payload = {
              termId: slipData.termId,
              userId: slipData.userId,
              slipId: slipData.slipId,
              ledger: [
                {
                  _id: currentItem._id || "",
                  number: number,
                  amount: amount,
                  showSummary: currentItem.showSummary,
                  groupId: currentItem.groupId || "",
                  summary: currentItem.summary,
                },
              ],
            };

            const response = await axiosPrivate.put(
              "/v1/slip/updateSlips",
              payload,
              {
                headers: {
                  "Content-Type": "application/json",
                  Authorization: `Bearer ${token}`,
                },
              }
            );

            if (response.data.code === "200") {
              toast.success(response.data?.message, { autoClose: 1000 });
              dispatch(closeModal());
              setRefersh(Math.random());
            } else {
              toast.error(response.data?.message, { autoClose: 1000 });
              setRefersh(Math.random());
            }
            dispatch(closeModal());
            return; // Exit early for single item amount changes
          }
        } else {
          // Fallback for items without groupId
          const payload = {
            termId: slipData.termId,
            userId: slipData.userId,
            slipId: slipData.slipId,
            ledger: [
              {
                _id: currentItem._id || "",
                number: number,
                amount: amount,
                showSummary: currentItem.showSummary,
                groupId: currentItem.groupId || "",
                summary: currentItem.summary,
              },
            ],
          };

          const response = await axiosPrivate.put(
            "/v1/slip/updateSlips",
            payload,
            {
              headers: {
                "Content-Type": "application/json",
                Authorization: `Bearer ${token}`,
              },
            }
          );

          if (response.data.code === "200") {
            toast.success(response.data?.message, { autoClose: 1000 });
            dispatch(closeModal());
            setRefersh(Math.random());
          } else {
            toast.error(response.data?.message, { autoClose: 1000 });
            setRefersh(Math.random());
          }
          dispatch(closeModal());
          return; // Exit early for single item amount changes
        }
      }

      let currentGroupId = null;

      if (currentSlipDetails && currentSlipDetails.items) {
        const currentItemInDetails = currentSlipDetails.items.find(
          (item) =>
            item._id === currentItem._id ||
            (item.number === currentItem.number &&
              item.amount === currentItem.amount)
        );
        currentGroupId = currentItemInDetails?.groupId;
      }

      const newGroupId = crypto.randomUUID
        ? crypto.randomUUID()
        : Date.now().toString() + Math.random().toString(36).substr(2, 9);

      let ledgerItems = [];

      if (currentSlipDetails && currentSlipDetails.items) {
        if (currentGroupId) {
          const itemsWithSameGroupId = currentSlipDetails.items.filter(
            (item) => item.groupId === currentGroupId
          );

          // Check if any item in the group has summary ending with "R", contains "ထိပ်", or ends with "B"
          const hasRPattern = itemsWithSameGroupId.some(
            (item) => item.summary && item.summary.endsWith("R")
          );
          const has3DRPattern = itemsWithSameGroupId.some(
            (item) =>
              item.summary &&
              item.summary.endsWith("R") &&
              item.number &&
              item.number.length === 3
          );
          const hasNStarPattern = itemsWithSameGroupId.some(
            (item) => item.summary && item.summary.includes("ထိပ်")
          );
          const hasBreakPattern = itemsWithSameGroupId.some(
            (item) =>
              item.summary &&
              (item.summary.endsWith("B") || item.summary.includes("ဘရိတ်"))
          );
          const hasStarNPattern = itemsWithSameGroupId.some(
            (item) => item.summary && item.summary.includes("ပိတ်")
          );
          const hasNPPattern = itemsWithSameGroupId.some(
            (item) => item.summary && item.summary.includes("ပါ")
          );
          const hasDoubleNStarPattern = itemsWithSameGroupId.some(
            (item) => item.summary && /^\d\d\*$/.test(item.summary)
          );
          const hasStartDoubleNPattern = itemsWithSameGroupId.some(
            (item) => item.summary && /^\*\d\d$/.test(item.summary)
          );
          const hasNStarNPattern = itemsWithSameGroupId.some(
            (item) => item.summary && /^\d\*\d$/.test(item.summary)
          );

          if (itemsWithSameGroupId.length > 0) {
            if (hasRPattern && number.length === 2) {
              // Then create new R pattern items
              const rPatternItems = handleRPatternUpdate(
                number,
                amount,
                currentGroupId
              );

              // Get all items from slip details that are NOT in the current group
              const otherItems = currentSlipDetails.items.filter(
                (item) => item.groupId !== currentGroupId
              );

              // Create R pattern items with proper _id
              const newRPatternItems = rPatternItems.map((item, index) => {
                // Use existing _id from itemsWithSameGroupId if available, otherwise generate new one
                const existingItem = itemsWithSameGroupId[index];
                return {
                  _id: existingItem
                    ? existingItem._id
                    : crypto.randomUUID
                    ? crypto.randomUUID()
                    : Date.now().toString() +
                      Math.random().toString(36).substr(2, 9),
                  number: item.number,
                  amount: item.amount,
                  showSummary: item.showSummary,
                  groupId: currentGroupId, // Reuse the same groupId
                  summary: item.summary,
                };
              });

              // Combine other items with new R pattern items
              ledgerItems = [...otherItems, ...newRPatternItems];
            } else if (hasNStarPattern) {
              // Check if we're updating the first digit or second digit
              const currentItemNumber = currentItem.number;
              const isFirstDigitUpdate = currentItemNumber[0] !== number[0];
              const isSecondDigitUpdate = currentItemNumber[1] !== number[1];

              if (isFirstDigitUpdate) {
                // If updating first digit, regenerate all 10 numbers
                const newFirstDigit = number[0];
                const nStarPatternItems = handleNStarPatternUpdate(
                  newFirstDigit,
                  amount,
                  currentGroupId
                );

                // Get all items from slip details that are NOT in the current group
                const otherItems = currentSlipDetails.items.filter(
                  (item) => item.groupId !== currentGroupId
                );

                // Create NStar pattern items with proper _id
                const newNStarPatternItems = nStarPatternItems.map(
                  (item, index) => {
                    // Use existing _id from itemsWithSameGroupId if available, otherwise generate new one
                    const existingItem = itemsWithSameGroupId[index];
                    return {
                      _id: existingItem
                        ? existingItem._id
                        : crypto.randomUUID
                        ? crypto.randomUUID()
                        : Date.now().toString() +
                          Math.random().toString(36).substr(2, 9),
                      number: item.number,
                      amount: item.amount,
                      showSummary: item.showSummary,
                      groupId: currentGroupId, // Reuse the same groupId
                      summary: item.summary,
                    };
                  }
                );

                // Combine other items with new NStar pattern items
                ledgerItems = [...otherItems, ...newNStarPatternItems];
              } else {
                // If updating second digit, use regular update logic
                ledgerItems = itemsWithSameGroupId.map((item) => {
                  if (
                    item._id === currentItem._id ||
                    (item.number === currentItem.number &&
                      item.amount === currentItem.amount)
                  ) {
                    return {
                      _id: item._id || "",
                      number: number,
                      amount: amount,
                      showSummary: "1",
                      groupId: crypto.randomUUID
                        ? crypto.randomUUID()
                        : Date.now().toString() +
                          Math.random().toString(36).substr(2, 9),
                      summary: number,
                    };
                  } else {
                    return {
                      _id: item._id || "",
                      number: item.number,
                      amount: item.amount,
                      showSummary: "1",
                      groupId: crypto.randomUUID
                        ? crypto.randomUUID()
                        : Date.now().toString() +
                          Math.random().toString(36).substr(2, 9),
                      summary: item.number,
                    };
                  }
                });
              }
            } else if (hasBreakPattern) {
              // For break pattern, we need to regenerate all numbers with the new end sum
              const endSum = parseInt(number, 10);
              const breakPatternItems = handleBreakPatternUpdate(
                endSum,
                amount,
                currentGroupId
              );

              // Get all items from slip details that are NOT in the current group
              const otherItems = currentSlipDetails.items.filter(
                (item) => item.groupId !== currentGroupId
              );

              // Create Break pattern items with proper _id
              const newBreakPatternItems = breakPatternItems.map(
                (item, index) => {
                  // Use existing _id from itemsWithSameGroupId if available, otherwise generate new one
                  const existingItem = itemsWithSameGroupId[index];
                  return {
                    _id: existingItem
                      ? existingItem._id
                      : crypto.randomUUID
                      ? crypto.randomUUID()
                      : Date.now().toString() +
                        Math.random().toString(36).substr(2, 9),
                    number: item.number,
                    amount: item.amount,
                    showSummary: item.showSummary,
                    groupId: currentGroupId, // Reuse the same groupId
                    summary: item.summary,
                  };
                }
              );

              // Combine other items with new Break pattern items
              ledgerItems = [...otherItems, ...newBreakPatternItems];
            } else if (hasStarNPattern) {
              // Check if we're updating the first digit or second digit
              const currentItemNumber = currentItem.number;
              const isFirstDigitUpdate = currentItemNumber[0] !== number[0];
              const isSecondDigitUpdate = currentItemNumber[1] !== number[1];

              if (isSecondDigitUpdate) {
                // If updating first digit, regenerate all 10 numbers
                const newLastDigit = number[1]; // Get the second digit (last digit for StarN)
                const starNPatternItems = handleStarNPatternUpdate(
                  newLastDigit,
                  amount,
                  currentGroupId
                );

                // Get all items from slip details that are NOT in the current group
                const otherItems = currentSlipDetails.items.filter(
                  (item) => item.groupId !== currentGroupId
                );

                // Create StarN pattern items with proper _id
                const newStarNPatternItems = starNPatternItems.map(
                  (item, index) => {
                    // Use existing _id from itemsWithSameGroupId if available, otherwise generate new one
                    const existingItem = itemsWithSameGroupId[index];
                    return {
                      _id: existingItem
                        ? existingItem._id
                        : crypto.randomUUID
                        ? crypto.randomUUID()
                        : Date.now().toString() +
                          Math.random().toString(36).substr(2, 9),
                      number: item.number,
                      amount: item.amount,
                      showSummary: item.showSummary,
                      groupId: currentGroupId, // Reuse the same groupId
                      summary: item.summary,
                    };
                  }
                );

                // Combine other items with new StarN pattern items
                ledgerItems = [...otherItems, ...newStarNPatternItems];
              } else {
                // If updating second digit, use regular update logic
                ledgerItems = itemsWithSameGroupId.map((item) => {
                  if (
                    item._id === currentItem._id ||
                    (item.number === currentItem.number &&
                      item.amount === currentItem.amount)
                  ) {
                    return {
                      _id: item._id || "",
                      number: number,
                      amount: amount,
                      showSummary: "1",
                      groupId: crypto.randomUUID
                        ? crypto.randomUUID()
                        : Date.now().toString() +
                          Math.random().toString(36).substr(2, 9),
                      summary: number,
                    };
                  } else {
                    return {
                      _id: item._id || "",
                      number: item.number,
                      amount: item.amount,
                      showSummary: "1",
                      groupId: crypto.randomUUID
                        ? crypto.randomUUID()
                        : Date.now().toString() +
                          Math.random().toString(36).substr(2, 9),
                      summary: item.number,
                    };
                  }
                });
              }
            } else if (has3DRPattern && number.length === 3) {
              // Handle 3-digit R pattern update

              // Check if any item in currentSlipDetails has showSummary "1"
              const hasUpdatedOne = currentSlipDetails.items.some(
                (item) => item.updated === "1"
              );

              if (hasUpdatedOne) {
                // Show Swal alert and prevent update
                Swal.fire({
                  title: "Update Not Allowed",
                  text: "Cannot update",
                  icon: "warning",
                  confirmButtonColor: "#3085d6",
                  confirmButtonText: "OK",
                });
                return; // Exit the function
              }

              // Check if any digits have changed
              const currentItemNumber = currentItem.number;
              const isFirstDigitUpdate = currentItemNumber[0] !== number[0];
              const isSecondDigitUpdate = currentItemNumber[1] !== number[1];
              const isThirdDigitUpdate = currentItemNumber[2] !== number[2];

              if (
                isFirstDigitUpdate ||
                isSecondDigitUpdate ||
                isThirdDigitUpdate
              ) {
                // Create new 3-digit R pattern items
                const threeDRPatternItems = handle3DRPatternUpdate(
                  number,
                  amount,
                  currentGroupId
                );

                // Delete old items that we don't reuse (items beyond the new count)
                try {
                  const newItemCount = threeDRPatternItems.length;
                  const oldItemsToDelete =
                    itemsWithSameGroupId.slice(newItemCount);

                  for (const oldItem of oldItemsToDelete) {
                    const deletePayload = {
                      _id: oldItem._id,
                      slipId: slipData.slipId,
                      termId: slipData.termId,
                      userId: slipData.userId,
                    };

                    const deleteResponse = await axiosPrivate.delete(
                      "/v1/slip/deleteSlip",
                      {
                        data: deletePayload,
                        headers: {
                          "Content-Type": "application/json",
                          Authorization: `Bearer ${token}`,
                        },
                      }
                    );

                    if (deleteResponse.data.code === "200") {
                      console.log(
                        "Successfully deleted old item:",
                        oldItem._id
                      );
                    } else {
                      console.log(
                        "Failed to delete old item:",
                        deleteResponse.data?.message
                      );
                    }
                  }
                } catch (error) {
                  console.log("Error deleting old items:", error);
                }

                // Get all items from slip details that are NOT in the current group
                const otherItems = currentSlipDetails.items.filter(
                  (item) => item.groupId !== currentGroupId
                );

                // Create 3-digit R pattern items with proper _id
                const newThreeDRPatternItems = threeDRPatternItems.map(
                  (item, index) => {
                    // Use existing _id from itemsWithSameGroupId if available, otherwise generate new one
                    const existingItem = itemsWithSameGroupId[index];
                    return {
                      _id: existingItem ? existingItem._id : "",
                      number: item.number,
                      amount: item.amount,
                      showSummary: item.showSummary,
                      groupId: currentGroupId, // Reuse the same groupId
                      summary: item.summary,
                    };
                  }
                );

                // Combine other items with new 3-digit R pattern items
                ledgerItems = [...otherItems, ...newThreeDRPatternItems];
              } else {
                // Use regular update logic if digits haven't changed
                ledgerItems = itemsWithSameGroupId.map((item) => {
                  if (
                    item._id === currentItem._id ||
                    (item.number === currentItem.number &&
                      item.amount === currentItem.amount)
                  ) {
                    return {
                      _id: item._id || "",
                      number: number,
                      amount: amount,
                      showSummary: "1",
                      groupId: crypto.randomUUID
                        ? crypto.randomUUID()
                        : Date.now().toString() +
                          Math.random().toString(36).substr(2, 9),
                      summary: number,
                    };
                  } else {
                    return {
                      _id: item._id || "",
                      number: item.number,
                      amount: item.amount,
                      showSummary: "1",
                      groupId: crypto.randomUUID
                        ? crypto.randomUUID()
                        : Date.now().toString() +
                          Math.random().toString(36).substr(2, 9),
                      summary: item.number,
                    };
                  }
                });
              }
            } else if (hasDoubleNStarPattern && number.length === 3) {
              // Check if first digit or second digit has changed
              const currentItemNumber = currentItem.number;
              const isFirstDigitUpdate = currentItemNumber[0] !== number[0];
              const isSecondDigitUpdate = currentItemNumber[1] !== number[1];
              const isThirdDigitUpdate = currentItemNumber[2] !== number[2];

              if (
                (isFirstDigitUpdate || isSecondDigitUpdate) &&
                !isThirdDigitUpdate
              ) {
                // Create new DoubleNStar pattern items
                const doubleNStarPatternItems = handleDoubleNStarPatternUpdate(
                  number[0] + "" + number[1],
                  amount,
                  currentGroupId
                );

                // Delete old items that we don't reuse (items beyond the new count)
                try {
                  const newItemCount = doubleNStarPatternItems.length;
                  const oldItemsToDelete =
                    itemsWithSameGroupId.slice(newItemCount);

                  for (const oldItem of oldItemsToDelete) {
                    const deletePayload = {
                      _id: oldItem._id,
                      slipId: slipData.slipId,
                      termId: slipData.termId,
                      userId: slipData.userId,
                    };

                    const deleteResponse = await axiosPrivate.delete(
                      "/v1/slip/deleteSlip",
                      {
                        data: deletePayload,
                        headers: {
                          "Content-Type": "application/json",
                          Authorization: `Bearer ${token}`,
                        },
                      }
                    );

                    if (deleteResponse.data.code === "200") {
                      console.log(
                        "Successfully deleted old item:",
                        oldItem._id
                      );
                    } else {
                      console.log(
                        "Failed to delete old item:",
                        deleteResponse.data?.message
                      );
                    }
                  }
                } catch (error) {
                  console.log("Error deleting old items:", error);
                }

                // Get all items from slip details that are NOT in the current group
                const otherItems = currentSlipDetails.items.filter(
                  (item) => item.groupId !== currentGroupId
                );

                // Create DoubleNStar pattern items with proper _id
                const newDoubleNStarPatternItems = doubleNStarPatternItems.map(
                  (item, index) => {
                    const existingItem = itemsWithSameGroupId[index];
                    return {
                      _id: existingItem ? existingItem._id : "",
                      number: item.number,
                      amount: item.amount,
                      showSummary: item.showSummary,
                      groupId: currentGroupId, // Reuse the same groupId
                      summary: item.summary,
                    };
                  }
                );

                // Combine other items with new DoubleNStar pattern items
                ledgerItems = [...otherItems, ...newDoubleNStarPatternItems];
              } else {
                // Use regular update logic if digits haven't changed
                ledgerItems = itemsWithSameGroupId.map((item) => {
                  if (
                    item._id === currentItem._id ||
                    (item.number === currentItem.number &&
                      item.amount === currentItem.amount)
                  ) {
                    return {
                      _id: item._id || "",
                      number: number,
                      amount: amount,
                      showSummary: "1",
                      groupId: crypto.randomUUID
                        ? crypto.randomUUID()
                        : Date.now().toString() +
                          Math.random().toString(36).substr(2, 9),
                      summary: number,
                    };
                  } else {
                    return {
                      _id: item._id || "",
                      number: item.number,
                      amount: item.amount,
                      showSummary: "1",
                      groupId: crypto.randomUUID
                        ? crypto.randomUUID()
                        : Date.now().toString() +
                          Math.random().toString(36).substr(2, 9),
                      summary: item.number,
                    };
                  }
                });
              }
            } else if (hasStartDoubleNPattern && number.length === 3) {
              // Check if first digit or second digit has changed
              const currentItemNumber = currentItem.number;
              const isFirstDigitUpdate = currentItemNumber[0] !== number[0];
              const isSecondDigitUpdate = currentItemNumber[1] !== number[1];
              const isThirdDigitUpdate = currentItemNumber[2] !== number[2];

              if (
                (isSecondDigitUpdate || isThirdDigitUpdate) &&
                !isFirstDigitUpdate
              ) {
                // Create new StartDoubleN pattern items
                const startDoubleNPatternItems =
                  handleStartDoubleNPatternUpdate(
                    number[1] + "" + number[2],
                    amount,
                    currentGroupId
                  );

                // Get all items from slip details that are NOT in the current group
                const otherItems = currentSlipDetails.items.filter(
                  (item) => item.groupId !== currentGroupId
                );

                // Create StartDoubleN pattern items with proper _id
                const newStartDoubleNPatternItems =
                  startDoubleNPatternItems.map((item, index) => {
                    const existingItem = itemsWithSameGroupId[index];
                    return {
                      _id: existingItem ? existingItem._id : "",
                      number: item.number,
                      amount: item.amount,
                      showSummary: item.showSummary,
                      groupId: currentGroupId, // Reuse the same groupId
                      summary: item.summary,
                    };
                  });

                // Combine other items with new StartDoubleN pattern items
                ledgerItems = [...otherItems, ...newStartDoubleNPatternItems];
              } else {
                // Use regular update logic if digits haven't changed
                ledgerItems = itemsWithSameGroupId.map((item) => {
                  if (
                    item._id === currentItem._id ||
                    (item.number === currentItem.number &&
                      item.amount === currentItem.amount)
                  ) {
                    return {
                      _id: item._id || "",
                      number: number,
                      amount: amount,
                      showSummary: "1",
                      groupId: crypto.randomUUID
                        ? crypto.randomUUID()
                        : Date.now().toString() +
                          Math.random().toString(36).substr(2, 9),
                      summary: number,
                    };
                  } else {
                    return {
                      _id: item._id || "",
                      number: item.number,
                      amount: item.amount,
                      showSummary: "1",
                      groupId: crypto.randomUUID
                        ? crypto.randomUUID()
                        : Date.now().toString() +
                          Math.random().toString(36).substr(2, 9),
                      summary: item.number,
                    };
                  }
                });
              }
            } else if (hasNStarNPattern && number.length === 3) {
              // Check if first digit or third digit has changed
              const currentItemNumber = currentItem.number;
              const isFirstDigitUpdate = currentItemNumber[0] !== number[0];
              const isSecondDigitUpdate = currentItemNumber[1] !== number[1];
              const isThirdDigitUpdate = currentItemNumber[2] !== number[2];

              if (
                (isFirstDigitUpdate || isThirdDigitUpdate) &&
                !isSecondDigitUpdate
              ) {
                // Create new NStarN pattern items
                const nStarNPatternItems = handleNStarNPatternUpdate(
                  number[0],
                  number[2],
                  amount,
                  currentGroupId
                );

                // Get all items from slip details that are NOT in the current group
                const otherItems = currentSlipDetails.items.filter(
                  (item) => item.groupId !== currentGroupId
                );

                // Create NStarN pattern items with proper _id
                const newNStarNPatternItems = nStarNPatternItems.map(
                  (item, index) => {
                    const existingItem = itemsWithSameGroupId[index];
                    return {
                      _id: existingItem ? existingItem._id : "",
                      number: item.number,
                      amount: item.amount,
                      showSummary: item.showSummary,
                      groupId: currentGroupId, // Reuse the same groupId
                      summary: item.summary,
                    };
                  }
                );

                // Combine other items with new NStarN pattern items
                ledgerItems = [...otherItems, ...newNStarNPatternItems];
              } else {
                // Use regular update logic if digits haven't changed
                ledgerItems = itemsWithSameGroupId.map((item) => {
                  if (
                    item._id === currentItem._id ||
                    (item.number === currentItem.number &&
                      item.amount === currentItem.amount)
                  ) {
                    return {
                      _id: item._id || "",
                      number: number,
                      amount: amount,
                      showSummary: "1",
                      groupId: crypto.randomUUID
                        ? crypto.randomUUID()
                        : Date.now().toString() +
                          Math.random().toString(36).substr(2, 9),
                      summary: number,
                    };
                  } else {
                    return {
                      _id: item._id || "",
                      number: item.number,
                      amount: item.amount,
                      showSummary: "1",
                      groupId: crypto.randomUUID
                        ? crypto.randomUUID()
                        : Date.now().toString() +
                          Math.random().toString(36).substr(2, 9),
                      summary: item.number,
                    };
                  }
                });
              }
            } else if (hasNPPattern) {
              // Extract the digit from the current item's summary (e.g., "1ပါ" → "1")
              const currentSummary = currentItem.summary;
              const currentDigit = currentSummary
                ? currentSummary.replace("ပါ", "")
                : "";

              // Check if the digit in the summary has been updated
              const isDigitUpdated =
                currentDigit !== number[0] && currentDigit !== number[1];

              if (isDigitUpdated) {
                // If the digit has been updated, use NP pattern logic
                // Use the digit that actually changed
                const digit =
                  currentDigit === number[0] ? number[1] : number[0];
                const npPatternItems = handleNPPatternUpdate(
                  digit,
                  amount,
                  currentGroupId
                );

                // Get all items from slip details that are NOT in the current group
                const otherItemsForNP = currentSlipDetails.items.filter(
                  (item) => item.groupId !== currentGroupId
                );

                // Create NP pattern items with proper _id
                const newNPPatternItems = npPatternItems.map((item, index) => {
                  // Use existing _id from itemsWithSameGroupId if available, otherwise generate new one
                  const existingItem = itemsWithSameGroupId[index];
                  return {
                    _id: existingItem
                      ? existingItem._id
                      : crypto.randomUUID
                      ? crypto.randomUUID()
                      : Date.now().toString() +
                        Math.random().toString(36).substr(2, 9),
                    number: item.number,
                    amount: item.amount,
                    showSummary: item.showSummary,
                    groupId: currentGroupId, // Reuse the same groupId
                    summary: item.summary,
                  };
                });

                // Combine other items with new NP pattern items
                ledgerItems = [...otherItemsForNP, ...newNPPatternItems];
              } else {
                // If the digit hasn't changed, use regular update logic
                ledgerItems = itemsWithSameGroupId.map((item) => {
                  if (
                    item._id === currentItem._id ||
                    (item.number === currentItem.number &&
                      item.amount === currentItem.amount)
                  ) {
                    return {
                      _id: item._id || "",
                      number: number,
                      amount: amount,
                      showSummary: "1",
                      groupId: crypto.randomUUID
                        ? crypto.randomUUID()
                        : Date.now().toString() +
                          Math.random().toString(36).substr(2, 9),
                      summary: number,
                    };
                  } else {
                    return {
                      _id: item._id || "",
                      number: item.number,
                      amount: item.amount,
                      showSummary: "1",
                      groupId: crypto.randomUUID
                        ? crypto.randomUUID()
                        : Date.now().toString() +
                          Math.random().toString(36).substr(2, 9),
                      summary: item.number,
                    };
                  }
                });
              }
            } else {
              // Regular update without R, NStar, Break, StarN, or NP pattern
              ledgerItems = itemsWithSameGroupId.map((item) => {
                if (
                  item._id === currentItem._id ||
                  (item.number === currentItem.number &&
                    item.amount === currentItem.amount)
                ) {
                  return {
                    _id: item._id || "",
                    number: number,
                    amount: amount,
                    showSummary: "1",
                    groupId: crypto.randomUUID
                      ? crypto.randomUUID()
                      : Date.now().toString() +
                        Math.random().toString(36).substr(2, 9),
                    summary: number,
                  };
                } else {
                  return {
                    _id: item._id || "",
                    number: item.number,
                    amount: item.amount,
                    showSummary: "1",
                    groupId: crypto.randomUUID
                      ? crypto.randomUUID()
                      : Date.now().toString() +
                        Math.random().toString(36).substr(2, 9),
                    summary: item.number,
                  };
                }
              });
            }
          } else {
            ledgerItems = [
              {
                _id: currentItem._id || "",
                number: number,
                amount: amount,
                showSummary: "1",
                groupId: newGroupId,
                summary: number,
              },
            ];
          }
        } else {
          // If groupId is null, only update the current item with its own unique groupId
          ledgerItems = [
            {
              _id: currentItem._id || "",
              number: number,
              amount: amount,
              showSummary: "1",
              groupId: crypto.randomUUID
                ? crypto.randomUUID()
                : Date.now().toString() +
                  Math.random().toString(36).substr(2, 9),
              summary: number,
            },
          ];
        }
      } else {
        ledgerItems = [
          {
            _id: currentItem._id || "",
            number: number,
            amount: amount,
            showSummary: "1",
            groupId: newGroupId,
            summary: number,
          },
        ];
      }

      const payload = {
        termId: slipData.termId,
        userId: slipData.userId,
        slipId: slipData.slipId,
        ledger: ledgerItems,
      };

      const response = await axiosPrivate.put("/v1/slip/updateSlips", payload, {
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${token}`,
        },
      });

      if (response.data.code === "200") {
        toast.success(response.data?.message, { autoClose: 1000 });
        dispatch(closeModal());
        setRefersh(Math.random());
      } else {
        toast.error(response.data?.message, { autoClose: 1000 });
        setRefersh(Math.random());
      }
      dispatch(closeModal());
    } catch (error) {
      console.log(error);
      toast.error(error, { autoClose: 1000 });
    }
  };
  const handleDeleteClick = (id, slip) => {
    Swal.fire({
      // title: "Are you sure?",
      text: "ရွေးချယ်ထားသော စလစ် ကိုဖျက်မှကျိန်းသေပါသလား?",
      icon: "warning",
      showCancelButton: true,
      confirmButtonColor: "#d33",
      cancelButtonColor: "#3085d6",
      confirmButtonText: "Yes, delete it!",
      focusConfirm: true,
      returnFocus: false,
    }).then((result) => {
      if (result.isConfirmed) {
        handleDeleteSlipAllData(id, slip);
      }
    });
  };

  const handleDeleteSlipAllData = async (index, item) => {
    try {
      await axiosPrivate
        .delete("/v1/slip/deleteSlip", {
          data: {
            slipId: item.slipId,
            termId: item.termId,
            userId: item.userId,
          },
          headers: {
            "Content-Type": "application/json",
            Authorization: `Bearer ${token}`,
          },
        })
        .then((response) => {
          toast.success(response.data?.message, { autoClose: 1000 });
          setSelectedSlip(null);
          setTotal(0);
          setSlips([]);
          setPage(1);
          setSlipRefresh(Math.random());
        })
        .catch((error) => {
          toast.error(error.response.data?.message, { autoClose: 1000 });
        });
    } catch (error) {
      console.log(error);
      toast.error("Error delteing slip:", { autoClose: 1000 });
    }
  };

  const formatCurrency = (value) => {
    const number = parseFloat(value);
    if (isNaN(number)) return value;
    return number.toLocaleString("en-US", { maximumFractionDigits: 0 });
  };

  const handleGoToSale = () => {
    if (termId) {
      navigate("/", { state: { termId: termId, userId: userId } });
    } else {
      toast.error("Please select a term first", { autoClose: 1000 });
    }
  };
  const removeStartPrefix = (input, prefix) => {
    if (input == null) return "";
    if(input==prefix) return input;
    if (input.startsWith(prefix)) {
      return input.slice(prefix.length);
    }
    return input;
  };
  return (
    <div className="bg-gray-100 h-screen grid grid-cols-5 w-screen">
      <div className="bg-white p-4 rounded shadow-md col-span-1 h-screen flex flex-col">
        <div className="mb-4">
          <div className="flex items-center space-x-2">
            {termOptions && termOptions.length > 0 ? (
              <Select
                placeholder="အပါတ်စဉ်"
                value={termOptions.find((opt) => opt.value === termId)}
                options={termOptions}
                onChange={handleTermNameChange}
                className="flex-grow mt-2"
              />
            ) : (
              ""
            )}

            <button
              onClick={handleGoToSale}
              className="mt-2 flex items-center gap-2 px-3 py-1 bg-red-600 text-white rounded hover:bg-red-700 transition"
            >
              <img src={saleIcon} alt="Sale" className="w-6 h-6" />
            </button>
          </div>
          <div className="flex items-center space-x-2">
            {users && users.length > 0 ? (
              <Select
                placeholder="ထိုးသား"
                value={users.find((opt) => opt.value === userId)}
                onChange={handleUserChange}
                options={users}
                className="flex-grow mt-2"
              />
            ) : (
              ""
            )}

            <button
              onClick={handleRefersh} // Uncomment to make it functional
              className="mt-2 flex items-center gap-2 px-4 py-2 bg-blue-500 text-white rounded hover:bg-blue-600 transition"
            >
              <TbRefresh size={18} />
            </button>
          </div>
        </div>

        <div className="space-y-3 w-full  my-2">
          <div className="flex items-center space-x-2 mb-4">
            <input
              type="checkbox"
              id="showAmount"
              checked={showAmount}
              onChange={(e) => setShowAmount(e.target.checked)}
              className="w-4 h-4"
            />
            <label htmlFor="showAmount" className="text-base font-semibold">
              Show Amount
            </label>
          </div>

          <div className="flex items-center space-x-3 w-full">
            <Input
                label="ယူနစ်ပေါင်း"
                value={formatCurrency(report?.TotalUnitWithDiscount)}
                disabled
              />
          </div>

          <div className="flex items-center space-x-3 w-full">
             <Input
                label="ငွေပေါင်း"
                value={formatCurrency(report?.TotalAmountWithoutDiscount)}
                disabled
              />
          </div>
          <div className="flex items-center space-x-3 w-full">
            <Input
                label="ပေါက်သီး"
                value={formatCurrency(report?.TotalWinAmountWithoutPrize)}
                disabled
              />
          </div>
          <div className="flex items-center space-x-3 w-full">
            <Input
                label="လျော်ကြေး"
                value={formatCurrency(report?.TotalAmountWithPrize)}
                disabled
              />
          </div>
          <div className="flex items-center space-x-3 w-full">
            <Input
                label="ပမာဏ"
                value={formatCurrency(report?.SubTotalAmount)}
                disabled
              />
          </div>
        </div>
      </div>

      <div className="bg-white p-2 rounded shadow-md h-screen col-span-1 overflow-y-auto">
        <div className="flex justify-between items-center mb-4">
          <span className="text-sm text-gray-500 font-bold">
            Total Slip: {total}
          </span>
          <span className="text-sm text-gray-500 font-bold">
            Total Unit : {totalAmountOfAllSlips.toLocaleString()}
          </span>
        </div>

        <div
          className="overflow-y-auto"
          style={{
            borderTop: "1px solid #e5e7eb", // Optional border for visual separation
            paddingTop: "8px",
          }}
        >
          {slips.map((slip, index) => (
            <div
              key={`${slip._id}${Math.random()}`}
              ref={slips.length === index + 1 ? lastSlipRef : null}
              className={`p-2 border rounded cursor-pointer mb-2 ${
                selectedSlip?.slipId == slip.slipId &&
                selectedSlip?.termId == slip.termId &&
                selectedSlip?.userId == slip.userId
                  ? "bg-blue-100 border-blue-500"
                  : "hover:bg-gray-100"
              }`}
              // style={{height: "200px"}}
              onClick={() => setSelectedSlip(slip)}
            >
              <div className="flex justify-between items-center">
                <div className="cursor-pointer flex-grow space-y-1">
                  <div className="flex justify-between text-xs font-sans">
                    <span className="font-normal">စလစ်နံပါတ်</span>
                    <span className="font-bold">{slip.slipId}</span>
                  </div>

                  <div className="flex justify-between text-xs">
                    <span className="font-normal">ထိုးသား</span>
                    <span className="font-bold">
                      {slip?.user?.name}
                      {slip?.creator
                        ? ` (Host-${removeStartPrefix(
                            slip?.creator,
                            userProfile?.businessName
                          )})`
                        : ""}
                    </span>
                  </div>

                  <div className="flex justify-between text-xs">
                    <span className="font-normal">မှတ်ချက်</span>
                    <span className="font-bold">{slip.customer}</span>
                  </div>
                  <div className="flex justify-between text-xs">
                    <span className="font-normal">Device</span>
                    <span className="font-bold">{slip?.deviceName}</span>
                  </div>

                  {/* <div className="flex justify-between text-xs ">
    <span className="font-normal">Host</span>
    <span className="font-bold">{}</span>
  </div> */}

                  <div className="flex justify-between text-xs mt-1">
                    <span className="font-normal">
                      {new Date(slip.createdAt).toLocaleString()}
                    </span>
                    {userProfile &&
                      ((userProfile?.userType == "employee" &&
                        userProfile.userAccess == 1) ||
                        userProfile?.userType === "owner") && (
                        <div className="flex gap-2 justify-end">
                          <button
                            onClick={() => {
                              handleDeleteClick(slip._id, slip);
                              // if (
                              //   window.confirm("ရွေးချယ်ထားသော စလစ် ကိုဖျက်မှကျိန်းသေပါသလား?")
                              // ) {
                              //   handleDeleteSlipAllData(slip._id, slip);
                              // }
                            }}
                            className="p-1 bg-red-500 text-white rounded hover:bg-red-600 flex items-center"
                            title="Delete Slip"
                          >
                            <TbTrash size={14} />
                          </button>

                          <button
                            onClick={() => {
                              setSelectedSlip(slip);
                              dispatch(openModal("update-slip-with-all-data"));
                            }}
                            className="p-1 bg-blue-500 text-white rounded hover:bg-blue-600 flex items-center"
                            title="Update Slip"
                          >
                            <TbUserHexagon size={14} />
                          </button>
                        </div>
                      )}
                  </div>
                </div>
              </div>
            </div>
          ))}

          {loading && (
            <div className="flex justify-center items-center h-40">
              <div className="animate-spin rounded-full h-10 w-10 border-t-4 border-blue-500"></div>
            </div>
          )}
        </div>
      </div>

      <div className="bg-transparent h-screen w-[75%] col-span-3">
        {loadingDetail && selectedSlip && (
          <div className="flex justify-center items-center h-40">
            <div className="animate-spin rounded-full h-10 w-10 border-t-4 border-blue-500"></div>
          </div>
        )}
        {!selectedSlip ? (
          <p>Select a slip to view its details.</p>
        ) : (
          slipDetails &&
          !loadingDetail && (
            <SlipDetail
              copy={slipDetails.copy}
              smsCopy={slipDetails.smsCopy}
              slipNumber={slipDetails.slipNumber}
              customerName={slipDetails.customerName}
              totalAmount={slipDetails.totalAmount}
              items={slipDetails.items}
              status={slipDetails.status}
              term={termOptions}
              termId={termId}
              userId={slipDetails.userId}
              phoneNumber={slipDetails.phoneNumber}
              onDelete={handleDeleteSingleSlip}
              slipRefresh={setRefersh}
              userRole={userProfile?.userType}
              userAccess={userProfile?.userAccess}
            />
          )
        )}
      </div>

      {modalMode === "update-slip" && slipData ? (
        <Modal needCloseButton>
          <UpdateSlipSingle
            number={slipData.number}
            amount={slipData.amount}
            handleUpdate={handleUpdateSingleSlip}
          />
        </Modal>
      ) : null}
      {modalMode === "update-slip-with-all-data" ? (
        <Modal needCloseButton>
          <ChangeSlipUserForm
            agentList={users}
            agent={selectedSlip?.userId}
            termId={termId}
            slip={selectedSlip}
            refreshSlip={setRefersh}
          />
        </Modal>
      ) : null}
    </div>
  );
};

export default Slip;

